package com.sap.engine.api.controller;

import com.sap.engine.persistence.repository.DataSourceRepository;
import com.sap.engine.persistence.repository.DiscoveredEntityRepository;
import com.sap.engine.persistence.repository.IngestionJobRepository;
import com.sap.engine.persistence.repository.RawRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DataSourceRepository dataSourceRepository;
    private final DiscoveredEntityRepository entityRepository;
    private final RawRecordRepository recordRepository;
    private final IngestionJobRepository jobRepository;

    public DashboardController(DataSourceRepository dataSourceRepository,
                               DiscoveredEntityRepository entityRepository,
                               RawRecordRepository recordRepository,
                               IngestionJobRepository jobRepository) {
        this.dataSourceRepository = dataSourceRepository;
        this.entityRepository = entityRepository;
        this.recordRepository = recordRepository;
        this.jobRepository = jobRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        long sourcesCount = dataSourceRepository.count();
        long entitiesCount = entityRepository.count();
        long recordsCount = recordRepository.count();
        long jobsCount = jobRepository.count();

        return ResponseEntity.ok(Map.of(
                "totalSources", sourcesCount,
                "totalEntitiesDiscovered", entitiesCount,
                "totalRecordsLanded", recordsCount,
                "totalIngestionJobs", jobsCount
        ));
    }
}
