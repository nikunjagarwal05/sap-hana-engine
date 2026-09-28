package com.sap.engine.persistence.repository;

import com.sap.engine.persistence.entity.TableProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableProfileRepository extends JpaRepository<TableProfileEntity, Long> {
    Optional<TableProfileEntity> findByEntityId(Long entityId);
}
