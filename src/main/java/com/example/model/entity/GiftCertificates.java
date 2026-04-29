package com.example.model.entity;

import com.example.constant.Enums;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "gift_certificates")
public class GiftCertificates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "ref_no", nullable = false, unique = true, length = 50)
    private String refNo;

    @Column(name = "promo_code", nullable = false, unique = true, length = 20)
    private String promoCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.GiftCertificateType type;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "remaining_quantity")
    private Integer remainingQuantity;

    @Column(name = "message_to_recipient", columnDefinition = "TEXT")
    private String messageToRecipient;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "giftCertificates", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GiftCertificateItems> items = new ArrayList<>();

    public boolean isUsable() {
        if (cancelledAt != null) return false;
        if (remainingQuantity < 1) return false;
        if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) return false;
        return true;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isCancelled() {
        return cancelledAt != null;
    }
}