package com.example.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class EventDayScheduleId implements Serializable {
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "day", length = 10, nullable = false)
    private String day;

    @Column(name = "start_time")
    private String startTime;
}