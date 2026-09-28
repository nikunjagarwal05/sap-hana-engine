package com.sap.engine.persistence.repository;

import com.sap.engine.persistence.entity.IngestionRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngestionRunRepository extends JpaRepository<IngestionRunEntity, Long> {
    List<IngestionRunEntity> findByJobId(Long jobId);
}
