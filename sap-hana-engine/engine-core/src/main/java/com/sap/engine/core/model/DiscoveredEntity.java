package com.sap.engine.core.model;

import java.util.List;

/**
 * Metadata representation of a table, view, or OData EntitySet discovered from an SAP source.
 */
public record DiscoveredEntity(
        String schemaName,
        String entityName,
        List<DiscoveredColumn> columns
) {
    public DiscoveredEntity {
        if (entityName == null || entityName.isBlank()) {
            throw new IllegalArgumentException("Entity name cannot be null or blank");
        }
        schemaName = (schemaName == null) ? "DEFAULT" : schemaName;
        columns = (columns == null) ? List.of() : List.copyOf(columns);
    }

    public String getFullQualifiedName() {
        return "DEFAULT".equals(schemaName) ? entityName : schemaName + "." + entityName;
    }
}
