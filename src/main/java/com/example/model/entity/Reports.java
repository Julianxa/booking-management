package com.example.model.entity;

import com.example.constant.Enums;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "reports")
public class Reports {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(name = "ref_no", unique = true, nullable = false)
    private String refNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private Enums.ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Enums.ReportStatus status;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "generated_by")
    private String generatedBy;

    @Column(name = "included_booking_events")
    private Integer includedBookingEvents;

    @Column(name = "total_booking_events_in_range")
    private Long totalBookingEventsInRange;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "created_at", updatable = false, nullable = false)
    private ZonedDateTime createdAt;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}
