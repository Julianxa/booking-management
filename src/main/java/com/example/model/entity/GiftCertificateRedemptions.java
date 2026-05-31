package com.example.model.entity;

import com.example.constant.Enums;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import java.time.ZonedDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "gift_certificate_redemptions")
public class GiftCertificateRedemptions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "gift_certificate_id", nullable = false)
    private Long giftCertificateId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "redeemed_by_user_id")
    private Long redeemedByUserId;

    @Column(name = "quantity_used", nullable = false)
    private Integer quantityUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Enums.GiftCertificateRedemptionStatus status;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "redeemed_at")
    private ZonedDateTime redeemedAt;
}
