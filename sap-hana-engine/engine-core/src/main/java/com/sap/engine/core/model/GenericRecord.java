package com.sap.engine.core.model;

import java.time.Instant;
import java.util.Map;

/**
 * Universal, schema-agnostic record representation landed into the system landing zone.
 * Avoids compile-time POJO generation by holding key-value property maps.
 */
public record GenericRecord(
        String sourceId,
        String entityName,
        Map<String, Object> fields,
        Instant fetchedAt
) {
    public GenericRecord {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId cannot be null or blank");
        }
        if (entityName == null || entityName.isBlank()) {
            throw new IllegalArgumentException("entityName cannot be null or blank");
        }
        fields = (fields == null) ? Map.of() : Map.copyOf(fields);
        fetchedAt = (fetchedAt == null) ? Instant.now() : fetchedAt;
    }
}
