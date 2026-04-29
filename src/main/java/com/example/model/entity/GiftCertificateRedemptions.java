package com.example.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(name = "redeemed_at", nullable = false)
    private LocalDateTime redeemedAt;
}
