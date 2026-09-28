package com.sap.engine.persistence.repository;

import com.sap.engine.persistence.entity.DiscoveredEntityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveredEntityRepository extends JpaRepository<DiscoveredEntityEntity, Long> {
    List<DiscoveredEntityEntity> findByDataSourceId(Long dataSourceId);
    Optional<DiscoveredEntityEntity> findByDataSourceIdAndSchemaNameAndEntityName(Long dataSourceId, String schemaName, String entityName);
}
