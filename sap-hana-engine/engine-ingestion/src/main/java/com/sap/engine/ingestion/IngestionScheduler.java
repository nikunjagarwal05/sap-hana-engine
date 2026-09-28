package com.sap.engine.ingestion;

import com.sap.engine.persistence.entity.DataSourceEntity;
import com.sap.engine.persistence.repository.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polling loop that regularly triggers ingestion for enabled data sources.
 */
@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final DataSourceRepository dataSourceRepository;
    private final IngestionEngine ingestionEngine;

    public IngestionScheduler(DataSourceRepository dataSourceRepository, IngestionEngine ingestionEngine) {
        this.dataSourceRepository = dataSourceRepository;
        this.ingestionEngine = ingestionEngine;
    }

    @Scheduled(fixedDelayString = "${engine.ingestion.poll-interval-ms:60000}")
    public void runIngestionLoop() {
        List<DataSourceEntity> enabledSources = dataSourceRepository.findByEnabledTrue();
        if (enabledSources.isEmpty()) return;

        log.info("Scheduler poll cycle started for {} active data sources...", enabledSources.size());

        for (DataSourceEntity source : enabledSources) {
            try {
                log.info("Triggering scheduled ingestion for source '{}' (ID: {})...", source.getName(), source.getId());
                ingestionEngine.triggerIngestion(source.getId());
            } catch (Exception e) {
                log.error("Scheduled ingestion failed for source '{}': {}", source.getName(), e.getMessage());
            }
        }
    }
}
