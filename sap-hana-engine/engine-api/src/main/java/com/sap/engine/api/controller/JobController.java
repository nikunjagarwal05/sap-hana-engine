package com.sap.engine.api.controller;

import com.sap.engine.persistence.entity.IngestionJobEntity;
import com.sap.engine.persistence.repository.IngestionJobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final IngestionJobRepository jobRepository;

    public JobController(IngestionJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping
    public ResponseEntity<List<IngestionJobEntity>> getJobHistory() {
        return ResponseEntity.ok(jobRepository.findTop20ByOrderByStartedAtDesc());
    }
}
