package com.example.model.entity;

import com.example.constant.Enums;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "event_day_schedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDaySchedules {

    @EmbeddedId
    private EventDayScheduleId id;

    @MapsId("eventId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, insertable = false, updatable = false)
    private Events event;

    public String getDay() {
        return id != null ? id.getDay() : null;
    }

    public Enums.Weekday getWeekday() {
        String day = id.getDay().toUpperCase();
        return switch (day) {
            case "MON", "MONDAY"   -> Enums.Weekday.MON;
            case "TUE", "TUESDAY"  -> Enums.Weekday.TUE;
            case "WED", "WEDNESDAY"-> Enums.Weekday.WED;
            case "THU", "THURSDAY" -> Enums.Weekday.THU;
            case "FRI", "FRIDAY"   -> Enums.Weekday.FRI;
            case "SAT", "SATURDAY" -> Enums.Weekday.SAT;
            case "SUN", "SUNDAY"   -> Enums.Weekday.SUN;
            default -> throw new IllegalArgumentException("Invalid day: " + day);
        };
    }
}
