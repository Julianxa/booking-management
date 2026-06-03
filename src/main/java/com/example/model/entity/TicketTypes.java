package com.example.model.entity;

import com.example.constant.Enums;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"event", "periods"})
@EqualsAndHashCode(of = "id")
@Table(name = "ticket_types")
public class TicketTypes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ref_no", nullable = false)
    private String refNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Events event;

    @OneToMany(mappedBy = "ticketTypes", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketPricePeriods> periods = new LinkedList<>();

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "name_zh_cn", length = 100)
    private String nameZhCn;

    @Column(name = "name_zh_hk", length = 100)
    private String nameZhHk;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "description_zh_cn", length = 255)
    private String descriptionZhCn;

    @Column(name = "description_zh_hk", length = 255)
    private String descriptionZhHk;

//    @Column(name = "capacity")
//    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Enums.TicketTypeStatus status;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    public void addTicketPricePeriods(TicketPricePeriods periods) {
        if (periods == null) return;
        periods.setTicketTypes(this);
        getTicketPricePeriods().add(periods);
    }

    public List<TicketPricePeriods> getTicketPricePeriods() {
        if (this.periods == null) {
            this.periods = new LinkedList<>();
        }
        return periods;
    }
}