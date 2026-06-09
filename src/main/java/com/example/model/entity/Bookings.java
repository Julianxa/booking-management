package com.example.model.entity;

import com.example.constant.Enums;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "bookings")
public class Bookings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "ref_no", unique = true, nullable = false)
    private String refNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Enums.BookingType type;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_paid_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPaidPrice;

    @Column(name = "gift_certificate_id", precision = 8, scale = 2)
    private Long giftCertificateId;

    @Column(name = "discount", precision = 8, scale = 2)
    private BigDecimal discount;

    @Column(name = "final_paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalPaidAmount;

    @Column(name = "currency")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Enums.BookingStatus status; // e.g., PENDING, CONFIRMED, CANCELLED, COMPLETED

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private Enums.Language language;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}
