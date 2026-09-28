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

@Entity
@Table(name = "discovered_column")
public class DiscoveredColumnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id", nullable = false)
    private DiscoveredEntityEntity entity;

    @Column(nullable = false)
    private String name;

    @Column(name = "inferred_type", nullable = false)
    private String inferredType;

    @Column(nullable = false)
    private boolean nullable = true;

    public DiscoveredColumnEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DiscoveredEntityEntity getEntity() { return entity; }
    public void setEntity(DiscoveredEntityEntity entity) { this.entity = entity; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getInferredType() { return inferredType; }
    public void setInferredType(String inferredType) { this.inferredType = inferredType; }
    public boolean isNullable() { return nullable; }
    public void setNullable(boolean nullable) { this.nullable = nullable; }
}
