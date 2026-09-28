package com.sap.engine.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.engine.connector.jdbc.JdbcHanaConnector;
import com.sap.engine.connector.rest.RestApiHanaConnector;
import com.sap.engine.core.connector.SapDataSourceConnector;
import com.sap.engine.core.model.DataSourceConfig;
import com.sap.engine.core.model.DiscoveredColumn;
import com.sap.engine.core.model.DiscoveredEntity;
import com.sap.engine.core.model.FetchOptions;
import com.sap.engine.core.model.GenericRecord;
import com.sap.engine.persistence.entity.DataSourceEntity;
import com.sap.engine.persistence.entity.DiscoveredColumnEntity;
import com.sap.engine.persistence.entity.DiscoveredEntityEntity;
import com.sap.engine.persistence.entity.IngestionJobEntity;
import com.sap.engine.persistence.entity.IngestionRunEntity;
import com.sap.engine.persistence.entity.RawRecordEntity;
import com.sap.engine.persistence.repository.DataSourceRepository;
import com.sap.engine.persistence.repository.DiscoveredEntityRepository;
import com.sap.engine.persistence.repository.IngestionJobRepository;
import com.sap.engine.persistence.repository.IngestionRunRepository;
import com.sap.engine.persistence.repository.RawRecordRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class IngestionEngine {

    private static final Logger log = LoggerFactory.getLogger(IngestionEngine.class);

    private final DataSourceRepository dataSourceRepository;
    private final DiscoveredEntityRepository entityRepository;
    private final RawRecordRepository recordRepository;
    private final IngestionJobRepository jobRepository;
    private final IngestionRunRepository runRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private final Map<String, SapDataSourceConnector> connectors = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public IngestionEngine(DataSourceRepository dataSourceRepository,
                           DiscoveredEntityRepository entityRepository,
                           RawRecordRepository recordRepository,
                           IngestionJobRepository jobRepository,
                           IngestionRunRepository runRepository,
                           ApplicationEventPublisher eventPublisher,
                           ObjectMapper objectMapper) {
        this.dataSourceRepository = dataSourceRepository;
        this.entityRepository = entityRepository;
        this.recordRepository = recordRepository;
        this.jobRepository = jobRepository;
        this.runRepository = runRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;

        // Register default pluggable connectors
        this.connectors.put("JDBC", new JdbcHanaConnector());
        this.connectors.put("REST", new RestApiHanaConnector());
    }

    @Transactional
    public IngestionJobEntity triggerIngestion(Long dataSourceId) {
        DataSourceEntity entity = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("DataSource not found with id: " + dataSourceId));

        DataSourceConfig config = parseConfig(entity);
        SapDataSourceConnector connector = connectors.get(config.connectorType().toUpperCase());
        if (connector == null) {
            throw new IllegalStateException("Unsupported connector type: " + config.connectorType());
        }

        IngestionJobEntity job = new IngestionJobEntity();
        job.setDataSource(entity);
        job.setStatus("RUNNING");
        job.setStartedAt(Instant.now());
        job = jobRepository.save(job);

        try {
            // 1. Auto-discover entities & diff catalog
            List<DiscoveredEntity> discovered = connector.discoverEntities(config);
            List<DiscoveredEntityEntity> savedEntities = syncCatalog(entity, discovered);
            job.setTotalEntities(savedEntities.size());

            // 2. Parallel ingestion execution across entities
            long grandTotalRecords = 0;
            List<Future<Long>> futures = new ArrayList<>();

            for (DiscoveredEntityEntity targetEntity : savedEntities) {
                IngestionJobEntity currentJob = job;
                DiscoveredEntity match = discovered.stream()
                        .filter(d -> d.entityName().equals(targetEntity.getEntityName()))
                        .findFirst().orElse(null);

                if (match != null) {
                    futures.add(executorService.submit(() -> ingestSingleEntity(config, connector, currentJob, targetEntity, match)));
                }
            }

            for (Future<Long> f : futures) {
                grandTotalRecords += f.get();
            }

            job.setTotalRecords(grandTotalRecords);
            job.setStatus("SUCCEEDED");
            job.setEndedAt(Instant.now());
            log.info("Ingestion job #{} completed successfully for source '{}'. Landed {} records.", job.getId(), entity.getName(), grandTotalRecords);

            // 3. Publish completion event for Statistical Analysis Engine
            eventPublisher.publishEvent(new IngestionCompletedEvent(job.getId(), dataSourceId));

        } catch (Exception e) {
            log.error("Ingestion job #{} failed for source '{}': {}", job.getId(), entity.getName(), e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setEndedAt(Instant.now());
        }

        return jobRepository.save(job);
    }

    private long ingestSingleEntity(DataSourceConfig config, SapDataSourceConnector connector, IngestionJobEntity job, DiscoveredEntityEntity targetEntity, DiscoveredEntity discoveredEntity) {
        IngestionRunEntity run = new IngestionRunEntity();
        run.setJob(job);
        run.setEntity(targetEntity);
        run.setEntityName(targetEntity.getEntityName());
        run.setStatus("RUNNING");
        run.setStartedAt(Instant.now());
        run = runRepository.save(run);

        long start = System.currentTimeMillis();
        long recordCount = 0;

        try {
            FetchOptions options = new FetchOptions(config.batchSize(), 0, null, 10000);
            
            // Clean old records for clean idempotency
            recordRepository.deleteByEntityId(targetEntity.getId());

            Retry retry = Retry.ofDefaults("sapRetry");
            List<GenericRecord> records = retry.executeSupplier(() -> connector.fetchRecords(config, discoveredEntity, options).toList());

            List<RawRecordEntity> entitiesToSave = new ArrayList<>();
            for (GenericRecord rec : records) {
                RawRecordEntity raw = new RawRecordEntity();
                raw.setEntity(targetEntity);
                raw.setRunId(run.getId());
                raw.setFieldsJson(objectMapper.writeValueAsString(rec.fields()));
                raw.setFetchedAt(rec.fetchedAt());
                entitiesToSave.add(raw);
            }

            recordRepository.saveAll(entitiesToSave);
            recordCount = entitiesToSave.size();

            run.setStatus("SUCCEEDED");
            run.setRecordsFetched(recordCount);
            run.setDurationMs(System.currentTimeMillis() - start);
            run.setEndedAt(Instant.now());

        } catch (Throwable t) {
            log.error("Failed ingesting entity '{}': {}", targetEntity.getEntityName(), t.getMessage());
            run.setStatus("FAILED");
            run.setErrorMessage(t.getMessage());
            run.setDurationMs(System.currentTimeMillis() - start);
            run.setEndedAt(Instant.now());
        }

        runRepository.save(run);
        return recordCount;
    }

    private List<DiscoveredEntityEntity> syncCatalog(DataSourceEntity sourceEntity, List<DiscoveredEntity> discovered) {
        List<DiscoveredEntityEntity> result = new ArrayList<>();

        for (DiscoveredEntity de : discovered) {
            Optional<DiscoveredEntityEntity> existing = entityRepository.findByDataSourceIdAndSchemaNameAndEntityName(
                    sourceEntity.getId(), de.schemaName(), de.entityName());

            DiscoveredEntityEntity entity = existing.orElseGet(() -> {
                DiscoveredEntityEntity newE = new DiscoveredEntityEntity();
                newE.setDataSource(sourceEntity);
                newE.setSchemaName(de.schemaName());
                newE.setEntityName(de.entityName());
                return newE;
            });

            entity.setDiscoveredAt(Instant.now());
            entity.getColumns().clear();

            for (DiscoveredColumn col : de.columns()) {
                DiscoveredColumnEntity colE = new DiscoveredColumnEntity();
                colE.setEntity(entity);
                colE.setName(col.name());
                colE.setInferredType(col.inferredType());
                colE.setNullable(col.nullable());
                entity.getColumns().add(colE);
            }

            result.add(entityRepository.save(entity));
        }

        return result;
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
            throw new IllegalArgumentException("Invalid DataSource JSON configuration: " + e.getMessage(), e);
        }
    }

    public record IngestionCompletedEvent(Long jobId, Long dataSourceId) {}
}
