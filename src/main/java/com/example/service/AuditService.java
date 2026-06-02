package com.example.service;

import com.example.model.entity.AuditTrail;
import com.example.repository.AuditTrailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditTrailRepository auditTrailRepository;

    public void record(String action, String entityType, Long entityId, Long userId, String payload) {
        try {
            AuditTrail a = new AuditTrail(action, entityType, entityId, userId, payload);
            auditTrailRepository.save(a);
        } catch (Exception e) {
            // Do not fail business flow if audit saving fails; log only
            System.err.println("Failed to record audit: " + e.getMessage());
        }
    }
}
