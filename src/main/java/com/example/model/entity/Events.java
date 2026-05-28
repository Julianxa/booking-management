package com.example.model.entity;

import com.example.constant.Enums;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"ticketTypes", "availableDays"})
@Table(name = "events")
public class Events {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;
    @Column(name = "ref_no", unique = true, nullable = false)
    private String refNo;
    @Column(name = "name", unique = true, nullable = false)
    private String name;
    @Column(name = "type")
    private String type;
    @Column(name = "category")
    private String category;
    @Column(name = "description")
    private String description;
    @Column(name = "location")
    private String location;
    @Column(name = "duration")
    private Integer duration;
    @Column(name = "badge")
    private String badge;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<EventDaySchedules> availableDays = new HashSet<>();
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketTypes> ticketTypes = new LinkedList<>();
    @Column(name = "equipment")
    private String equipment;
    @Column(name = "availability_to_employee_ratio")
    private Integer availabilityToEmployeeRatio;
    @Column(name = "event_pic_key")
    private String eventPicKey;
    @Column(name = "max_capacity")
    private Integer maxCapacity;
    @Column(name = "private_bookings")
    private Boolean privateBookings;
    @Column(name = "additional_info")
    private String additionalInfo;
    @Column(name = "cancellation_policy")
    private String cancellationPolicy;
    @Column(name = "custom_question")
    private String customQuestion;
    @Column(name = "match_ticket_quantity_with_attendees")
    private Boolean matchTicketQuantityWithAttendees;
    @Column(name = "is_publish")
    private Boolean isPublish;
    @Column(name = "activity_day_threshold")
    private Integer activityDayThreshold;
    @Column(name = "activity_hour_threshold")
    private Integer activityHourThreshold;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Enums.EventStatus status;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "updated_by")
    private Long updatedBy;
    @Column(name = "deleted_by")
    private Long deletedBy;
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "updated_at", nullable = false)
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

    public void updateDay(Long eventId, String day, List<String> startTimes) {
        startTimes.stream()
                .map(startTime -> EventDayScheduleId.builder()
                        .eventId(eventId)
                        .day(day)
                        .startTime(startTime)
                        .build())
                .map(id -> EventDaySchedules.builder()
                        .id(id)
                        .event(this)
                        .build())
                .forEach(this.getAvailableDays()::add);
    }

    public Set<EventDaySchedules> getAvailableDays() {
        if (this.availableDays == null) {
            this.availableDays = new HashSet<>();
        }
        return availableDays;
    }

    public void addTicketType(TicketTypes ticketType) {
        if (ticketType == null) return;
        ticketType.setEvent(this);
        getTicketTypes().add(ticketType);
    }

    public List<TicketTypes> getTicketTypes() {
        if (this.ticketTypes == null) {
            this.ticketTypes = new LinkedList<>();
        }
        return ticketTypes;
    }
}
