package com.sap.engine.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.engine.ingestion.IngestionEngine;
import com.sap.engine.persistence.entity.ColumnProfileEntity;
import com.sap.engine.persistence.entity.DiscoveredColumnEntity;
import com.sap.engine.persistence.entity.DiscoveredEntityEntity;
import com.sap.engine.persistence.entity.RawRecordEntity;
import com.sap.engine.persistence.entity.TableProfileEntity;
import com.sap.engine.persistence.repository.DiscoveredEntityRepository;
import com.sap.engine.persistence.repository.RawRecordRepository;
import com.sap.engine.persistence.repository.TableProfileRepository;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Dynamic Statistical Analysis Engine.
 * Automatically profiles landed JSONB records across any discovered SAP table or OData entity set.
 */
@Service
public class StatisticalAnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(StatisticalAnalysisEngine.class);

    private final DiscoveredEntityRepository entityRepository;
    private final RawRecordRepository recordRepository;
    private final TableProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public StatisticalAnalysisEngine(DiscoveredEntityRepository entityRepository,
                                     RawRecordRepository recordRepository,
                                     TableProfileRepository profileRepository,
                                     ObjectMapper objectMapper) {
        this.entityRepository = entityRepository;
        this.recordRepository = recordRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener
    @Transactional
    public void onIngestionCompleted(IngestionEngine.IngestionCompletedEvent event) {
        log.info("Received IngestionCompletedEvent for job #{}. Starting automated analysis for source ID: {}", event.jobId(), event.dataSourceId());
        List<DiscoveredEntityEntity> entities = entityRepository.findByDataSourceId(event.dataSourceId());
        for (DiscoveredEntityEntity entity : entities) {
            profileEntity(entity);
        }
    }

    @Transactional
    public TableProfileEntity profileEntity(DiscoveredEntityEntity entity) {
        log.info("Running statistical analysis on entity '{}' (ID: {})...", entity.getEntityName(), entity.getId());

        List<RawRecordEntity> records = recordRepository.findByEntityId(entity.getId());
        long totalRows = records.size();

        TableProfileEntity tableProfile = profileRepository.findByEntityId(entity.getId())
                .orElseGet(() -> {
                    TableProfileEntity tp = new TableProfileEntity();
                    tp.setEntity(entity);
                    return tp;
                });

        tableProfile.setRowCount(totalRows);
        tableProfile.setLastIngestedAt(records.isEmpty() ? Instant.now() : records.get(0).getFetchedAt());
        tableProfile.setProfiledAt(Instant.now());
        tableProfile.getColumnProfiles().clear();

        if (totalRows == 0) {
            return profileRepository.save(tableProfile);
        }

        // Parse JSON fields into in-memory row maps
        List<Map<String, Object>> parsedRows = new ArrayList<>();
        for (RawRecordEntity r : records) {
            try {
                Map<String, Object> map = objectMapper.readValue(r.getFieldsJson(), Map.class);
                parsedRows.add(map);
            } catch (Exception ignored) {}
        }

        // Profile each discovered column
        for (DiscoveredColumnEntity col : entity.getColumns()) {
            ColumnProfileEntity colProfile = analyzeColumn(tableProfile, col.getName(), parsedRows, totalRows);
            tableProfile.getColumnProfiles().add(colProfile);
        }

        log.info("Completed analysis for entity '{}'. Profiled {} columns over {} rows.", entity.getEntityName(), entity.getColumns().size(), totalRows);
        return profileRepository.save(tableProfile);
    }

    private ColumnProfileEntity analyzeColumn(TableProfileEntity tableProfile, String colName, List<Map<String, Object>> rows, long totalRows) {
        ColumnProfileEntity cp = new ColumnProfileEntity();
        cp.setTableProfile(tableProfile);
        cp.setColumnName(colName);

        List<Object> nonNullValues = new ArrayList<>();
        int nullCount = 0;

        for (Map<String, Object> row : rows) {
            Object val = row.get(colName);
            if (val == null) {
                nullCount++;
            } else {
                nonNullValues.add(val);
            }
        }

        double nullPct = (totalRows == 0) ? 0.0 : ((double) nullCount / totalRows) * 100.0;
        cp.setNullPercentage(nullPct);

        Set<Object> distinctSet = new HashSet<>(nonNullValues);
        cp.setDistinctCount(distinctSet.size());

        String semanticType = inferSemanticType(nonNullValues);
        cp.setInferredSemanticType(semanticType);

        if ("NUMERIC".equals(semanticType)) {
            analyzeNumeric(cp, nonNullValues);
        } else if ("CATEGORICAL".equals(semanticType) || "TEXT".equals(semanticType)) {
            analyzeCategorical(cp, nonNullValues);
        } else if ("TEMPORAL".equals(semanticType)) {
            analyzeTemporal(cp, nonNullValues);
        }

        return cp;
    }

    private String inferSemanticType(List<Object> values) {
        if (values.isEmpty()) return "TEXT";

        int numericCount = 0;
        int dateCount = 0;
        int booleanCount = 0;

        for (Object obj : values) {
            if (obj instanceof Number) {
                numericCount++;
            } else if (obj instanceof Boolean) {
                booleanCount++;
            } else if (obj instanceof String str) {
                if (str.matches("^\\d+(\\.\\d+)?$")) {
                    numericCount++;
                } else if (str.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                    dateCount++;
                } else if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) {
                    booleanCount++;
                }
            }
        }

        int sampleSize = values.size();
        if ((double) numericCount / sampleSize > 0.8) return "NUMERIC";
        if ((double) dateCount / sampleSize > 0.8) return "TEMPORAL";
        if ((double) booleanCount / sampleSize > 0.8) return "BOOLEAN";
        return "CATEGORICAL";
    }

    private void analyzeNumeric(ColumnProfileEntity cp, List<Object> values) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        List<Double> doubleVals = new ArrayList<>();

        for (Object obj : values) {
            try {
                double val = Double.parseDouble(obj.toString());
                stats.addValue(val);
                doubleVals.add(val);
            } catch (NumberFormatException ignored) {}
        }

        if (stats.getN() > 0) {
            cp.setMinVal(String.valueOf(stats.getMin()));
            cp.setMaxVal(String.valueOf(stats.getMax()));
            cp.setMeanVal(stats.getMean());
            cp.setStddevVal(stats.getStandardDeviation());

            // Z-Score Outlier Detection (|z| > 3.0)
            double mean = stats.getMean();
            double stddev = stats.getStandardDeviation();
            int outliers = 0;
            if (stddev > 0.00001) {
                for (double d : doubleVals) {
                    double zScore = Math.abs((d - mean) / stddev);
                    if (zScore > 3.0) outliers++;
                }
            }
            cp.setOutlierCount(outliers);
        }
    }

    private void analyzeCategorical(ColumnProfileEntity cp, List<Object> values) {
        Map<String, Long> freqMap = values.stream()
                .collect(Collectors.groupingBy(Object::toString, Collectors.counting()));

        // Keep Top 10 frequencies
        Map<String, Long> top10 = freqMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        try {
            cp.setTopFrequenciesJson(objectMapper.writeValueAsString(top10));
        } catch (Exception ignored) {}
    }

    private void analyzeTemporal(ColumnProfileEntity cp, List<Object> values) {
        Map<String, Long> dateBuckets = new TreeMap<>();
        String minDate = null;
        String maxDate = null;

        for (Object obj : values) {
            String str = obj.toString();
            if (str.length() >= 10) {
                String dateDay = str.substring(0, 10);
                dateBuckets.merge(dateDay, 1L, Long::sum);

                if (minDate == null || dateDay.compareTo(minDate) < 0) minDate = dateDay;
                if (maxDate == null || dateDay.compareTo(maxDate) > 0) maxDate = dateDay;
            }
        }

        cp.setMinVal(minDate);
        cp.setMaxVal(maxDate);
        try {
            cp.setTimeBucketsJson(objectMapper.writeValueAsString(dateBuckets));
        } catch (Exception ignored) {}
    }
}
