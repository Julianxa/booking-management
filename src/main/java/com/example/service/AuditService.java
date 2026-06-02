package com.example.service;

import com.example.model.entity.AuditTrail;
import com.example.repository.AuditTrailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    private final AuditTrailRepository auditTrailRepository;

    public void record(String action, String entityType, Long entityId, Long userId, String payload) {
        try {
            AuditTrail a = new AuditTrail(action, entityType, entityId, userId, payload);
            auditTrailRepository.save(a);
        } catch (Exception e) {
            log.error("Failed to record audit", e);
        }
    }
}
