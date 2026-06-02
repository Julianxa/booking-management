package com.example.model.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "audit_trail")
public class AuditTrail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;

    private String entityType;

    private Long entityId;

    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private ZonedDateTime createdAt = ZonedDateTime.now();

    public AuditTrail() {}

    public AuditTrail(String action, String entityType, Long entityId, Long userId, String payload) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.userId = userId;
        this.payload = payload;
        this.createdAt = ZonedDateTime.now();
    }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public Long getUserId() { return userId; }
    public String getPayload() { return payload; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
}
