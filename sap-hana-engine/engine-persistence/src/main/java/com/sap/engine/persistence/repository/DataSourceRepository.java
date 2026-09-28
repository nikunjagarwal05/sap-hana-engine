package com.sap.engine.persistence.repository;

import com.sap.engine.persistence.entity.DataSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSourceEntity, Long> {
    List<DataSourceEntity> findByEnabledTrue();
}
