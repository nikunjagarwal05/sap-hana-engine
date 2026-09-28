package com.sap.engine.api.controller;

import com.sap.engine.persistence.entity.RawRecordEntity;
import com.sap.engine.persistence.entity.TableProfileEntity;
import com.sap.engine.persistence.repository.RawRecordRepository;
import com.sap.engine.persistence.repository.TableProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/entities")
public class EntityController {

    private final TableProfileRepository profileRepository;
    private final RawRecordRepository recordRepository;

    public EntityController(TableProfileRepository profileRepository, RawRecordRepository recordRepository) {
        this.profileRepository = profileRepository;
        this.recordRepository = recordRepository;
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<TableProfileEntity> getProfile(@PathVariable Long id) {
        return profileRepository.findByEntityId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/records")
    public ResponseEntity<Page<RawRecordEntity>> getRecords(@PathVariable Long id,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(recordRepository.findByEntityId(id, PageRequest.of(page, size)));
    }
}
