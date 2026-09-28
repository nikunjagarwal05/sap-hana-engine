package com.sap.engine.persistence.repository;

import com.sap.engine.persistence.entity.IngestionJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngestionJobRepository extends JpaRepository<IngestionJobEntity, Long> {
    List<IngestionJobEntity> findTop20ByOrderByStartedAtDesc();
    List<IngestionJobEntity> findByDataSourceIdOrderByStartedAtDesc(Long dataSourceId);
}
