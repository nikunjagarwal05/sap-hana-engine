package com.sap.engine.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discovered_entity")
public class DiscoveredEntityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private DataSourceEntity dataSource;

    @Column(name = "schema_name", nullable = false)
    private String schemaName;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "discovered_at")
    private Instant discoveredAt = Instant.now();

    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiscoveredColumnEntity> columns = new ArrayList<>();

    public DiscoveredEntityEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DataSourceEntity getDataSource() { return dataSource; }
    public void setDataSource(DataSourceEntity dataSource) { this.dataSource = dataSource; }
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public Instant getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(Instant discoveredAt) { this.discoveredAt = discoveredAt; }
    public List<DiscoveredColumnEntity> getColumns() { return columns; }
    public void setColumns(List<DiscoveredColumnEntity> columns) { this.columns = columns; }
}
