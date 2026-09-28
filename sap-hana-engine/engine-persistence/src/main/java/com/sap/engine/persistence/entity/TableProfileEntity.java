package com.sap.engine.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "table_profile")
public class TableProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    private DiscoveredEntityEntity entity;

    @Column(name = "row_count", nullable = false)
    private long rowCount = 0;

    @Column(name = "last_ingested_at")
    private Instant lastIngestedAt;

    @Column(name = "profiled_at")
    private Instant profiledAt = Instant.now();

    @OneToMany(mappedBy = "tableProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ColumnProfileEntity> columnProfiles = new ArrayList<>();

    public TableProfileEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DiscoveredEntityEntity getEntity() { return entity; }
    public void setEntity(DiscoveredEntityEntity entity) { this.entity = entity; }
    public long getRowCount() { return rowCount; }
    public void setRowCount(long rowCount) { this.rowCount = rowCount; }
    public Instant getLastIngestedAt() { return lastIngestedAt; }
    public void setLastIngestedAt(Instant lastIngestedAt) { this.lastIngestedAt = lastIngestedAt; }
    public Instant getProfiledAt() { return profiledAt; }
    public void setProfiledAt(Instant profiledAt) { this.profiledAt = profiledAt; }
    public List<ColumnProfileEntity> getColumnProfiles() { return columnProfiles; }
    public void setColumnProfiles(List<ColumnProfileEntity> columnProfiles) { this.columnProfiles = columnProfiles; }
}
