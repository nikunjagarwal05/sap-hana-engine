package com.sap.engine.connector.rest;

import com.sap.engine.core.model.DiscoveredColumn;
import com.sap.engine.core.model.DiscoveredEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for OData EDMX $metadata XML service definitions.
 * Auto-discovers EntitySets and Property column schemas.
 */
public class EdmxMetadataParser {

    private static final Logger log = LoggerFactory.getLogger(EdmxMetadataParser.class);

    public static List<DiscoveredEntity> parseEdmxMetadata(String edmxXml) {
        List<DiscoveredEntity> discoveredEntities = new ArrayList<>();
        Map<String, List<DiscoveredColumn>> entityTypesMap = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Protect against XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(edmxXml.getBytes(StandardCharsets.UTF_8)));

            // 1. Parse EntityType definitions
            NodeList entityTypeNodes = doc.getElementsByTagNameNS("*", "EntityType");
            for (int i = 0; i < entityTypeNodes.getLength(); i++) {
                Element entityTypeEl = (Element) entityTypeNodes.item(i);
                String typeName = entityTypeEl.getAttribute("Name");
                
                List<DiscoveredColumn> columns = new ArrayList<>();
                NodeList propNodes = entityTypeEl.getElementsByTagNameNS("*", "Property");
                for (int j = 0; j < propNodes.getLength(); j++) {
                    Element propEl = (Element) propNodes.item(j);
                    String propName = propEl.getAttribute("Name");
                    String edmType = propEl.getAttribute("Type");
                    boolean nullable = !"false".equalsIgnoreCase(propEl.getAttribute("Nullable"));
                    
                    columns.add(new DiscoveredColumn(propName, mapEdmType(edmType), nullable));
                }
                entityTypesMap.put(typeName, columns);
            }

            // 2. Parse EntitySet bindings
            NodeList entitySetNodes = doc.getElementsByTagNameNS("*", "EntitySet");
            for (int i = 0; i < entitySetNodes.getLength(); i++) {
                Element entitySetEl = (Element) entitySetNodes.item(i);
                String setName = entitySetEl.getAttribute("Name");
                String entityTypeAttr = entitySetEl.getAttribute("EntityType");
                
                // Extract unqualified entity type name
                String shortTypeName = entityTypeAttr.contains(".") 
                        ? entityTypeAttr.substring(entityTypeAttr.lastIndexOf('.') + 1) 
                        : entityTypeAttr;

                List<DiscoveredColumn> columns = entityTypesMap.getOrDefault(shortTypeName, List.of());
                discoveredEntities.add(new DiscoveredEntity("OData", setName, columns));
            }

        } catch (Exception e) {
            log.error("Failed to parse OData EDMX metadata XML: {}", e.getMessage(), e);
        }

        return discoveredEntities;
    }

    private static String mapEdmType(String edmType) {
        if (edmType == null) return "VARCHAR";
        return switch (edmType) {
            case "Edm.Int16", "Edm.Int32", "Edm.Byte" -> "INTEGER";
            case "Edm.Int64" -> "BIGINT";
            case "Edm.Double", "Edm.Single" -> "DOUBLE";
            case "Edm.Decimal" -> "DECIMAL";
            case "Edm.Boolean" -> "BOOLEAN";
            case "Edm.DateTime", "Edm.DateTimeOffset", "Edm.Time" -> "TIMESTAMP";
            case "Edm.Guid" -> "UUID";
            default -> "VARCHAR";
        };
    }
}
