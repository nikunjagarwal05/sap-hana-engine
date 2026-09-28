package com.sap.engine.connector.jdbc;

import com.sap.engine.core.connector.SapDataSourceConnector;
import com.sap.engine.core.exception.ConnectorException;
import com.sap.engine.core.model.DataSourceConfig;
import com.sap.engine.core.model.DiscoveredColumn;
import com.sap.engine.core.model.DiscoveredEntity;
import com.sap.engine.core.model.FetchOptions;
import com.sap.engine.core.model.GenericRecord;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Direct SQL Connector for SAP HANA Cloud (and generic JDBC compliant databases).
 * Uses ngdbc driver, HikariCP connection pooling, and SAP catalog view introspection.
 */
public class JdbcHanaConnector implements SapDataSourceConnector {

    private static final Logger log = LoggerFactory.getLogger(JdbcHanaConnector.class);

    private static final Set<String> SAP_SYSTEM_SCHEMAS = Set.of(
            "SYS", "_SYS_BI", "_SYS_BIC", "_SYS_RT", "_SYS_DI", "_SYS_REPO",
            "SYSTEM", "SAP_PA_APL", "HANA_XS_BASE", "INFORMATION_SCHEMA"
    );

    private final Map<Long, HikariDataSource> dataSourcePools = new ConcurrentHashMap<>();

    @Override
    public String getConnectorType() {
        return "JDBC";
    }

    @Override
    public boolean testConnection(DataSourceConfig config) {
        try (Connection conn = createDirectConnection(config)) {
            return conn != null && conn.isValid(5);
        } catch (Exception e) {
            log.error("JDBC Connection test failed for source '{}': {}", config.name(), e.getMessage());
            return false;
        }
    }

    @Override
    public List<DiscoveredEntity> discoverEntities(DataSourceConfig config) {
        log.info("Starting schema discovery for JDBC source '{}'...", config.name());
        List<DiscoveredEntity> entities = new ArrayList<>();

        try (Connection conn = getConnectionFromPool(config)) {
            DatabaseMetaData metaData = conn.getMetaData();
            String productName = metaData.getDatabaseProductName();

            if (productName != null && productName.toUpperCase().contains("HANA")) {
                entities.addAll(discoverHanaSystemViews(conn, config));
            } else {
                entities.addAll(discoverGenericMetadata(conn, config));
            }
        } catch (SQLException e) {
            throw new ConnectorException("Failed entity discovery for source: " + config.name(), e);
        }

        log.info("Discovered {} entities for source '{}'", entities.size(), config.name());
        return entities;
    }

    @Override
    public Stream<GenericRecord> fetchRecords(DataSourceConfig config, DiscoveredEntity entity, FetchOptions options) {
        log.info("Fetching records for entity '{}' limit={} offset={}", entity.getFullQualifiedName(), options.limit(), options.offset());
        
        String sql = buildSelectSql(entity, options);
        
        try {
            Connection conn = getConnectionFromPool(config);
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            Spliterator<GenericRecord> spliterator = new Spliterators.AbstractSpliterator<>(
                    Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
                
                private int count = 0;

                @Override
                public boolean tryAdvance(Consumer<? super GenericRecord> action) {
                    try {
                        if (count >= options.limit() || !rs.next()) {
                            closeResources(rs, ps, conn);
                            return false;
                        }

                        Map<String, Object> fields = new HashMap<>(columnCount);
                        for (int i = 1; i <= columnCount; i++) {
                            String colName = rsmd.getColumnLabel(i);
                            Object val = rs.getObject(i);
                            fields.put(colName, val);
                        }

                        GenericRecord record = new GenericRecord(
                                String.valueOf(config.id()),
                                entity.getFullQualifiedName(),
                                fields,
                                Instant.now()
                        );
                        
                        count++;
                        action.accept(record);
                        return true;

                    } catch (SQLException e) {
                        closeResources(rs, ps, conn);
                        throw new ConnectorException("Error advancing JDBC result set for entity " + entity.entityName(), e);
                    }
                }
            };

            return StreamSupport.stream(spliterator, false)
                    .onClose(() -> closeResources(rs, ps, conn));

        } catch (SQLException e) {
            throw new ConnectorException("Failed executing query for entity " + entity.entityName(), e);
        }
    }

    // ─── Private Introspection & Query Helpers ───────────────────────

    private List<DiscoveredEntity> discoverHanaSystemViews(Connection conn, DataSourceConfig config) throws SQLException {
        List<DiscoveredEntity> entities = new ArrayList<>();
        String sql = """
            SELECT T.SCHEMA_NAME, T.TABLE_NAME, C.COLUMN_NAME, C.DATA_TYPE_NAME, C.IS_NULLABLE
            FROM SYS.TABLES T
            JOIN SYS.TABLE_COLUMNS C ON T.SCHEMA_NAME = C.SCHEMA_NAME AND T.TABLE_NAME = C.TABLE_NAME
            ORDER BY T.SCHEMA_NAME, T.TABLE_NAME, C.POSITION
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String currentSchema = null;
            String currentTable = null;
            List<DiscoveredColumn> cols = new ArrayList<>();

            while (rs.next()) {
                String schema = rs.getString("SCHEMA_NAME");
                String table = rs.getString("TABLE_NAME");
                String colName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("DATA_TYPE_NAME");
                boolean nullable = "TRUE".equalsIgnoreCase(rs.getString("IS_NULLABLE"));

                if (shouldSkipSchema(schema, config)) continue;

                if (currentTable != null && (!table.equals(currentTable) || !schema.equals(currentSchema))) {
                    entities.add(new DiscoveredEntity(currentSchema, currentTable, List.copyOf(cols)));
                    cols.clear();
                }

                currentSchema = schema;
                currentTable = table;
                cols.add(new DiscoveredColumn(colName, dataType, nullable));
            }

            if (currentTable != null) {
                entities.add(new DiscoveredEntity(currentSchema, currentTable, List.copyOf(cols)));
            }
        }
        return entities;
    }

    private List<DiscoveredEntity> discoverGenericMetadata(Connection conn, DataSourceConfig config) throws SQLException {
        List<DiscoveredEntity> entities = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();

        try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE", "VIEW"})) {
            while (tables.next()) {
                String schema = tables.getString("TABLE_SCHEM");
                String table = tables.getString("TABLE_NAME");
                schema = (schema == null) ? "PUBLIC" : schema;

                if (shouldSkipSchema(schema, config)) continue;

                List<DiscoveredColumn> cols = new ArrayList<>();
                try (ResultSet columns = metaData.getColumns(null, schema, table, "%")) {
                    while (columns.next()) {
                        String colName = columns.getString("COLUMN_NAME");
                        String typeName = columns.getString("TYPE_NAME");
                        boolean nullable = "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"));
                        cols.add(new DiscoveredColumn(colName, typeName, nullable));
                    }
                }
                entities.add(new DiscoveredEntity(schema, table, cols));
            }
        }
        return entities;
    }

    private boolean shouldSkipSchema(String schema, DataSourceConfig config) {
        if (config.schemaAllowList() != null && !config.schemaAllowList().isEmpty()) {
            return !config.schemaAllowList().contains(schema);
        }
        if (config.schemaDenyList() != null && config.schemaDenyList().contains(schema)) {
            return true;
        }
        return SAP_SYSTEM_SCHEMAS.contains(schema.toUpperCase());
    }

    private String buildSelectSql(DiscoveredEntity entity, FetchOptions options) {
        String schema = entity.schemaName();
        String table = entity.entityName();
        if ("DEFAULT".equals(schema) || "PUBLIC".equals(schema)) {
            return String.format("SELECT * FROM \"%s\" LIMIT %d OFFSET %d", table, options.limit(), options.offset());
        }
        return String.format("SELECT * FROM \"%s\".\"%s\" LIMIT %d OFFSET %d", schema, table, options.limit(), options.offset());
    }

    private Connection createDirectConnection(DataSourceConfig config) throws SQLException {
        HikariConfig hikariConfig = createHikariConfig(config);
        hikariConfig.setMaximumPoolSize(1);
        try (HikariDataSource ds = new HikariDataSource(hikariConfig)) {
            return ds.getConnection();
        }
    }

    private Connection getConnectionFromPool(DataSourceConfig config) throws SQLException {
        HikariDataSource ds = dataSourcePools.computeIfAbsent(config.id(), id -> new HikariDataSource(createHikariConfig(config)));
        return ds.getConnection();
    }

    private HikariConfig createHikariConfig(DataSourceConfig config) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(config.jdbcUrl());
        hc.setUsername(config.username());
        hc.setPassword(config.password());
        hc.setPoolName("Hikari-Pool-SAP-JDBC-" + config.id());
        hc.setMaximumPoolSize(10);
        hc.setMinimumIdle(2);
        hc.setConnectionTimeout(30000);
        hc.setIdleTimeout(600000);
        return hc;
    }

    private void closeResources(ResultSet rs, PreparedStatement ps, Connection conn) {
        try { if (rs != null && !rs.isClosed()) rs.close(); } catch (Exception ignored) {}
        try { if (ps != null && !ps.isClosed()) ps.close(); } catch (Exception ignored) {}
        try { if (conn != null && !conn.isClosed()) conn.close(); } catch (Exception ignored) {}
    }
}
