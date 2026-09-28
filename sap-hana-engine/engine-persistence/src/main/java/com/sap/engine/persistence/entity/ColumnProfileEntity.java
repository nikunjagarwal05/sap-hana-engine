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
@Table(name = "column_profile")
public class ColumnProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_profile_id", nullable = false)
    private TableProfileEntity tableProfile;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "null_percentage", nullable = false)
    private double nullPercentage = 0.0;

    @Column(name = "distinct_count", nullable = false)
    private long distinctCount = 0;

    @Column(name = "inferred_semantic_type", nullable = false)
    private String inferredSemanticType; // 'NUMERIC', 'CATEGORICAL', 'TEMPORAL', 'TEXT', 'BOOLEAN'

    @Column(name = "min_val")
    private String minVal;

    @Column(name = "max_val")
    private String maxVal;

    @Column(name = "mean_val")
    private Double meanVal;

    @Column(name = "stddev_val")
    private Double stddevVal;

    @Column(name = "outlier_count")
    private int outlierCount = 0;

    @Column(name = "top_frequencies_json", columnDefinition = "jsonb")
    private String topFrequenciesJson;

    @Column(name = "time_buckets_json", columnDefinition = "jsonb")
    private String timeBucketsJson;

    public ColumnProfileEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TableProfileEntity getTableProfile() { return tableProfile; }
    public void setTableProfile(TableProfileEntity tableProfile) { this.tableProfile = tableProfile; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public double getNullPercentage() { return nullPercentage; }
    public void setNullPercentage(double nullPercentage) { this.nullPercentage = nullPercentage; }
    public long getDistinctCount() { return distinctCount; }
    public void setDistinctCount(long distinctCount) { this.distinctCount = distinctCount; }
    public String getInferredSemanticType() { return inferredSemanticType; }
    public void setInferredSemanticType(String inferredSemanticType) { this.inferredSemanticType = inferredSemanticType; }
    public String getMinVal() { return minVal; }
    public void setMinVal(String minVal) { this.minVal = minVal; }
    public String getMaxVal() { return maxVal; }
    public void setMaxVal(String maxVal) { this.maxVal = maxVal; }
    public Double getMeanVal() { return meanVal; }
    public void setMeanVal(Double meanVal) { this.meanVal = meanVal; }
    public Double getStddevVal() { return stddevVal; }
    public void setStddevVal(Double stddevVal) { this.stddevVal = stddevVal; }
    public int getOutlierCount() { return outlierCount; }
    public void setOutlierCount(int outlierCount) { this.outlierCount = outlierCount; }
    public String getTopFrequenciesJson() { return topFrequenciesJson; }
    public void setTopFrequenciesJson(String topFrequenciesJson) { this.topFrequenciesJson = topFrequenciesJson; }
    public String getTimeBucketsJson() { return timeBucketsJson; }
    public void setTimeBucketsJson(String timeBucketsJson) { this.timeBucketsJson = timeBucketsJson; }
}
