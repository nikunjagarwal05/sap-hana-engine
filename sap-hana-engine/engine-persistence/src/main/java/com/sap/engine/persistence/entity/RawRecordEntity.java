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
@Table(name = "raw_record")
public class RawRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    private DiscoveredEntityEntity entity;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "fields", nullable = false, columnDefinition = "jsonb")
    private String fieldsJson;

    @Column(name = "fetched_at")
    private Instant fetchedAt = Instant.now();

    public RawRecordEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DiscoveredEntityEntity getEntity() { return entity; }
    public void setEntity(DiscoveredEntityEntity entity) { this.entity = entity; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
}
