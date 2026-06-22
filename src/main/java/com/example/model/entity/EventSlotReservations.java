package com.example.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "event_slot_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSlotReservations {
    @EmbeddedId
    private EventSlotReservationId id;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
}
