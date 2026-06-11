package com.example.model.entity;

import com.example.constant.Enums;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Builder
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booking_events")
public class BookingEvents {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "ref_no", unique = true, nullable = false)
    private String refNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Bookings booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Events event;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", nullable = false)
    private String eventTime;

    @Column(name = "notes")
    private String notes;

    @Column(name = "answer")
    private String answer;

    @Column(name = "total")
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Enums.BookingEventStatus status;

    @Column(name = "verification_token", nullable = false)
    private String verificationToken;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "verified_at")
    private ZonedDateTime verifiedAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "reminder_sent_at")
    private ZonedDateTime reminderSentAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "cancelled_at")
    private ZonedDateTime cancelledAt;

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
