package com.sap.engine.connector.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sap.engine.core.connector.SapDataSourceConnector;
import com.sap.engine.core.exception.ConnectorException;
import com.sap.engine.core.model.DataSourceConfig;
import com.sap.engine.core.model.DiscoveredEntity;
import com.sap.engine.core.model.FetchOptions;
import com.sap.engine.core.model.GenericRecord;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * REST / OData Connector for SAP API Business Hub and SAP HANA OData v2/v4 Services.
 */
public class RestApiHanaConnector implements SapDataSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(RestApiHanaConnector.class);

    private final ObjectMapper mapper;
    private final CloseableHttpClient httpClient;

    public RestApiHanaConnector() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public String getConnectorType() {
        return "REST";
    }

    @Override
    public boolean testConnection(DataSourceConfig config) {
        String metadataUrl = config.restBaseUrl().endsWith("/") 
                ? config.restBaseUrl() + "$metadata" 
                : config.restBaseUrl() + "/$metadata";
        
        HttpGet request = new HttpGet(metadataUrl);
        applyAuthHeaders(request, config);

        try (ClassicHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            return statusCode >= 200 && statusCode < 300;
        } catch (Exception e) {
            log.error("REST connection test failed for source '{}': {}", config.name(), e.getMessage());
            return false;
        }
    }

    @Override
    public List<DiscoveredEntity> discoverEntities(DataSourceConfig config) {
        String metadataUrl = config.restBaseUrl().endsWith("/") 
                ? config.restBaseUrl() + "$metadata" 
                : config.restBaseUrl() + "/$metadata";

        HttpGet request = new HttpGet(metadataUrl);
        applyAuthHeaders(request, config);
        request.setHeader("Accept", "application/xml, text/xml");

        try (ClassicHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new ConnectorException("Failed fetching OData $metadata. HTTP status: " + statusCode);
            }
            HttpEntity entity = response.getEntity();
            String xmlContent = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            return EdmxMetadataParser.parseEdmxMetadata(xmlContent);

        } catch (Exception e) {
            throw new ConnectorException("Error discovering OData entities for source " + config.name(), e);
        }
    }

    @Override
    public Stream<GenericRecord> fetchRecords(DataSourceConfig config, DiscoveredEntity entity, FetchOptions options) {
        String entityUrl = buildEntityUrl(config, entity, options);
        HttpGet request = new HttpGet(entityUrl);
        applyAuthHeaders(request, config);
        request.setHeader("Accept", "application/json");

        try {
            ClassicHttpResponse response = httpClient.execute(request);
            int statusCode = response.getCode();
            if (statusCode < 200 || statusCode >= 300) {
                response.close();
                throw new ConnectorException("Failed REST record fetch. HTTP status: " + statusCode + " URL: " + entityUrl);
            }

            HttpEntity httpEntity = response.getEntity();
            String jsonContent = EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);
            response.close();

            JsonNode root = mapper.readTree(jsonContent);
            JsonNode recordsArray = extractRecordsArray(root);

            if (recordsArray == null || !recordsArray.isArray()) {
                return Stream.empty();
            }

            Iterator<JsonNode> iterator = recordsArray.elements();
            Spliterator<JsonNode> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);

            return StreamSupport.stream(spliterator, false).map(node -> {
                Map<String, Object> fields = new HashMap<>();
                node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), jsonNodeToObject(entry.getValue())));

                return new GenericRecord(
                        String.valueOf(config.id()),
                        entity.entityName(),
                        fields,
                        Instant.now()
                );
            });

        } catch (Exception e) {
            throw new ConnectorException("Failed executing OData request for entity " + entity.entityName(), e);
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────

    private void applyAuthHeaders(HttpGet request, DataSourceConfig config) {
        if ("API_KEY".equalsIgnoreCase(config.authType()) && config.apiKey() != null) {
            request.setHeader("APIKey", config.apiKey());
        } else if ("BASIC".equalsIgnoreCase(config.authType()) && config.username() != null) {
            String credentials = config.username() + ":" + (config.password() != null ? config.password() : "");
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            request.setHeader("Authorization", "Basic " + encoded);
        } else if ("OAUTH2_CLIENT_CREDENTIALS".equalsIgnoreCase(config.authType())) {
            String token = acquireOAuthToken(config);
            if (token != null) {
                request.setHeader("Authorization", "Bearer " + token);
            }
        }
    }

    private String acquireOAuthToken(DataSourceConfig config) {
        if (config.tokenUrl() == null || config.clientId() == null) return null;
        HttpPost post = new HttpPost(config.tokenUrl());
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "client_credentials"));
        params.add(new BasicNameValuePair("client_id", config.clientId()));
        params.add(new BasicNameValuePair("client_secret", config.clientSecret()));
        post.setEntity(new UrlEncodedFormEntity(params));

        try (ClassicHttpResponse response = httpClient.execute(post)) {
            String json = EntityUtils.toString(response.getEntity());
            JsonNode root = mapper.readTree(json);
            return root.has("access_token") ? root.get("access_token").asText() : null;
        } catch (Exception e) {
            log.error("Failed acquiring OAuth2 token for source '{}': {}", config.name(), e.getMessage());
            return null;
        }
    }

    private String buildEntityUrl(DataSourceConfig config, DiscoveredEntity entity, FetchOptions options) {
        String base = config.restBaseUrl().endsWith("/") ? config.restBaseUrl() : config.restBaseUrl() + "/";
        String url = base + entity.entityName() + "?$top=" + options.limit() + "&$skip=" + options.offset() + "&$count=true";
        if (options.skiptoken() != null && !options.skiptoken().isBlank()) {
            url += "&$skiptoken=" + options.skiptoken();
        }
        return url;
    }

    private JsonNode extractRecordsArray(JsonNode root) {
        if (root.has("value")) return root.get("value"); // OData v4
        if (root.has("d")) {
            JsonNode d = root.get("d");
            if (d.has("results")) return d.get("results"); // OData v2
            if (d.isArray()) return d;
        }
        if (root.isArray()) return root;
        return null;
    }

    private Object jsonNodeToObject(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isBoolean()) return node.asBoolean();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble() || node.isFloat()) return node.asDouble();
        if (node.isTextual()) return node.asText();
        return node.toString();
    }
}
