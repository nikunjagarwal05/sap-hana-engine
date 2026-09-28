package com.sap.engine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.engine.connector.jdbc.JdbcHanaConnector;
import com.sap.engine.connector.rest.RestApiHanaConnector;
import com.sap.engine.core.connector.SapDataSourceConnector;
import com.sap.engine.core.model.DataSourceConfig;
import com.sap.engine.ingestion.IngestionEngine;
import com.sap.engine.persistence.entity.DataSourceEntity;
import com.sap.engine.persistence.entity.DiscoveredEntityEntity;
import com.sap.engine.persistence.entity.IngestionJobEntity;
import com.sap.engine.persistence.repository.DataSourceRepository;
import com.sap.engine.persistence.repository.DiscoveredEntityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sources")
public class DataSourceController {

    private final DataSourceRepository dataSourceRepository;
    private final DiscoveredEntityRepository entityRepository;
    private final IngestionEngine ingestionEngine;
    private final ObjectMapper objectMapper;

    public DataSourceController(DataSourceRepository dataSourceRepository,
                                DiscoveredEntityRepository entityRepository,
                                IngestionEngine ingestionEngine,
                                ObjectMapper objectMapper) {
        this.dataSourceRepository = dataSourceRepository;
        this.entityRepository = entityRepository;
        this.ingestionEngine = ingestionEngine;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<DataSourceEntity>> listSources() {
        return ResponseEntity.ok(dataSourceRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<DataSourceEntity> createSource(@RequestBody DataSourceEntity source) {
        if (source.getConfigJson() == null || source.getConfigJson().isBlank()) {
            source.setConfigJson("{}");
        }
        DataSourceEntity saved = dataSourceRepository.save(source);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/test-connection")
    public ResponseEntity<?> testConnection(@PathVariable Long id) {
        DataSourceEntity source = dataSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));

        DataSourceConfig config = parseConfig(source);
        SapDataSourceConnector connector = "JDBC".equalsIgnoreCase(source.getConnectorType())
                ? new JdbcHanaConnector() : new RestApiHanaConnector();

        boolean success = connector.testConnection(config);
        return ResponseEntity.ok(Map.of("success", success, "message", success ? "Connection successful" : "Connection failed"));
    }

    @PostMapping("/{id}/ingest")
    public ResponseEntity<IngestionJobEntity> triggerIngestion(@PathVariable Long id) {
        IngestionJobEntity job = ingestionEngine.triggerIngestion(id);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/{id}/entities")
    public ResponseEntity<List<DiscoveredEntityEntity>> getEntities(@PathVariable Long id) {
        return ResponseEntity.ok(entityRepository.findByDataSourceId(id));
    }

    private DataSourceConfig parseConfig(DataSourceEntity entity) {
        try {
            Map<String, Object> map = objectMapper.readValue(entity.getConfigJson(), Map.class);
            return new DataSourceConfig(
                    entity.getId(),
                    entity.getName(),
                    entity.getConnectorType(),
                    (String) map.get("jdbcUrl"),
                    (String) map.get("username"),
                    (String) map.get("password"),
                    (String) map.get("restBaseUrl"),
                    (String) map.get("apiKey"),
                    (String) map.getOrDefault("authType", "NONE"),
                    (String) map.get("clientId"),
                    (String) map.get("clientSecret"),
                    (String) map.get("tokenUrl"),
                    (List<String>) map.get("schemaAllowList"),
                    (List<String>) map.get("schemaDenyList"),
                    entity.getPollIntervalSeconds(),
                    10,
                    500,
                    Map.of()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid config JSON: " + e.getMessage());
        }
    }
}
