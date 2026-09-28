package com.sap.engine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ingestion_run")
public class IngestionRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private IngestionJobEntity job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    private DiscoveredEntityEntity entity;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(nullable = false)
    private String status; // 'RUNNING', 'SUCCEEDED', 'FAILED'

    @Column(name = "records_fetched")
    private long recordsFetched = 0;

    @Column(name = "duration_ms")
    private long durationMs = 0;

    @Column(name = "started_at")
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "error_message")
    private String errorMessage;

    public IngestionRunEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public IngestionJobEntity getJob() { return job; }
    public void setJob(IngestionJobEntity job) { this.job = job; }
    public DiscoveredEntityEntity getEntity() { return entity; }
    public void setEntity(DiscoveredEntityEntity entity) { this.entity = entity; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getRecordsFetched() { return recordsFetched; }
    public void setRecordsFetched(long recordsFetched) { this.recordsFetched = recordsFetched; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
