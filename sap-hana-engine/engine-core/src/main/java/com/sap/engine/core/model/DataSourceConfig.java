package com.sap.engine.core.model;

import java.util.List;
import java.util.Map;

/**
 * Immutable configuration holder for a SAP Data Source (JDBC or REST/OData).
 */
public record DataSourceConfig(
        Long id,
        String name,
        String connectorType, // "JDBC" or "REST"
        
        // JDBC Parameters
        String jdbcUrl,
        String username,
        String password,
        
        // REST / OData Parameters
        String restBaseUrl,
        String apiKey,
        String authType, // "API_KEY", "OAUTH2_CLIENT_CREDENTIALS", "BASIC", "NONE"
        String clientId,
        String clientSecret,
        String tokenUrl,
        
        // Schema Introspection Options
        List<String> schemaAllowList,
        List<String> schemaDenyList,
        
        // Polling & Rate Limiting
        int pollIntervalSeconds,
        int rateLimitRps,
        int batchSize,
        
        // Additional Headers / Properties
        Map<String, String> customProperties
) {
    public DataSourceConfig {
        if (connectorType == null || connectorType.isBlank()) {
            throw new IllegalArgumentException("connectorType cannot be null or blank");
        }
        schemaAllowList = schemaAllowList == null ? List.of() : List.copyOf(schemaAllowList);
        schemaDenyList = schemaDenyList == null ? List.of() : List.copyOf(schemaDenyList);
        customProperties = customProperties == null ? Map.of() : Map.copyOf(customProperties);
        if (pollIntervalSeconds <= 0) pollIntervalSeconds = 300; // Default 5 mins
        if (rateLimitRps <= 0) rateLimitRps = 10;
        if (batchSize <= 0) batchSize = 500;
    }
}
