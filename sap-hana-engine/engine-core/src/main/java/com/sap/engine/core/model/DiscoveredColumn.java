package com.sap.engine.core.model;

/**
 * Metadata representation of a column/property within a discovered SAP table or OData entity set.
 */
public record DiscoveredColumn(
        String name,
        String inferredType, // "INTEGER", "DOUBLE", "VARCHAR", "TIMESTAMP", "BOOLEAN", "DECIMAL", etc.
        boolean nullable
) {
    public DiscoveredColumn {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name cannot be null or blank");
        }
        if (inferredType == null || inferredType.isBlank()) {
            inferredType = "VARCHAR";
        }
    }
}
