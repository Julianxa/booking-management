package com.example.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "password_reset_sessions")
public class PasswordResetSessions {
    public enum Status {
        PENDING_OTP,
        OTP_VERIFIED,
        CONSUMED,
        SUPERSEDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "consumed_at")
    private ZonedDateTime consumedAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.failedAttempts < 0) {
            this.failedAttempts = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}
