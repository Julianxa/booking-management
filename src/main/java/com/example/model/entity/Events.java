package com.example.model.entity;

import com.example.constant.Enums;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
    @Column(name = "match_ticket_quantity_with_attendees")
    private Boolean matchTicketQuantityWithAttendees;
    @Column(name = "is_publish")
    private Boolean isPublish;
    @Column(name = "min_activity_threshold_time")
    private Double minActivityThresholdTime;
    @Column(name = "max_activity_threshold_time")
    private Double maxActivityThresholdTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Enums.EventStatus status;
    @Column(name = "created_by")
    private Long createdBy;
    @Column(name = "updated_by")
    private Long updatedBy;
    @Column(name = "deleted_by")
    private Long deletedBy;
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name="deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt   = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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
