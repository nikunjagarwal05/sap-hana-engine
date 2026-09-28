package com.sap.engine.core.connector;

import com.sap.engine.core.model.DataSourceConfig;
import com.sap.engine.core.model.DiscoveredEntity;
import com.sap.engine.core.model.FetchOptions;
import com.sap.engine.core.model.GenericRecord;

import java.util.List;
import java.util.stream.Stream;

/**
 * Universal source-agnostic contract implemented by all SAP connectors (JDBC, REST/OData).
 */
public interface SapDataSourceConnector {

    /**
     * Identifies the connector protocol type.
     * @return "JDBC" or "REST"
     */
    String getConnectorType();

    /**
     * Validates connection parameters and credentials against the target SAP system.
     * @param config The data source configuration containing connection details
     * @return true if connection succeeds, false otherwise
     */
    boolean testConnection(DataSourceConfig config);

    /**
     * Auto-discovers all available tables, views, or OData entity sets exposed by the data source.
     * @param config The data source configuration
     * @return List of discovered entity schemas and their column metadata
     */
    List<DiscoveredEntity> discoverEntities(DataSourceConfig config);

    /**
     * Streams generic records from a specified discovered entity without hand-written POJOs.
     * @param config The data source configuration
     * @param entity The target entity to read from
     * @param options Pagination, batching, and filter options
     * @return Stream of generic record rows tagged with metadata
     */
    Stream<GenericRecord> fetchRecords(DataSourceConfig config, DiscoveredEntity entity, FetchOptions options);
}
