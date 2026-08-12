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
@Table(name = "octo_booking_mappings")
public class OctoBookingMappings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "octo_uuid", nullable = false, unique = true, length = 64)
    private String octoUuid;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "booking_ref_no", nullable = false, length = 64)
    private String bookingRefNo;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(name = "option_id", nullable = false, length = 64)
    private String optionId;

    @Column(name = "availability_id", nullable = false, length = 512)
    private String availabilityId;

    @Column(name = "reseller_reference", length = 255)
    private String resellerReference;

    @Column(name = "octo_status", nullable = false, length = 32)
    private String octoStatus;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "hold_expires_at")
    private ZonedDateTime holdExpiresAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "confirmed_at")
    private ZonedDateTime confirmedAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }
}
