package com.sap.engine.persistence.repository;

import com.sap.engine.persistence.entity.RawRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawRecordRepository extends JpaRepository<RawRecordEntity, Long> {
    Page<RawRecordEntity> findByEntityId(Long entityId, Pageable pageable);
    List<RawRecordEntity> findByEntityId(Long entityId);
    long countByEntityId(Long entityId);
    void deleteByEntityId(Long entityId);
}
