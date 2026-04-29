package com.example.model.dto;


import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateEventResponseDTO {
    @Schema(description = "Unique identifier of the created event", example = "123")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Name of the event/activity")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Type of event")
    @JsonProperty("type")
    private String type;

    @Schema(description = "Category or tag")
    @JsonProperty("category")
    private String category;

    @Schema(description = "Detailed description")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Location")
    @JsonProperty("location")
    private String location;

    @Schema(description = "Duration")
    @JsonProperty("duration")
    private Integer duration;

    @Schema(description = "Badge")
    @JsonProperty("badge")
    private String badge;

    @Schema(
            description = "Start date and time of the event (with timezone)"
    )
    @JsonProperty("start_date")
    private LocalDate startDate;

    @Schema(
            description = "End date and time of the event (with timezone)"
    )
    @JsonProperty("end_date")
    private LocalDate endDate;

    @Schema(description = "Explicit list of allowed days. ['MON','TUE','WED','THU','FRI','SAT','SUN']")
    @JsonProperty("available_days")
    private Set<AvailableDayDTO> availableDays;

    @JsonProperty("ticket_types")
    List<CreateBookingRequestDTO.TicketTypeDTO> ticketTypes;

    @Schema(description = "Required equipment")
    @JsonProperty("equipment")
    private String equipment;

    @JsonProperty("event_pic_url")
    private String eventPicUrl;

    @Schema(description = "Availability to employee/staff ratio")
    @JsonProperty("availability_to_employee_ratio")
    private Integer availabilityToEmployeeRatio;

    @Schema(description = "Overall maximum capacity")
    @JsonProperty("max_capacity")
    private Integer maxCapacity;

    @Schema(description = "Whether private/group bookings are allowed")
    @JsonProperty("private_bookings")
    private Boolean privateBookings;

    @Schema(description = "Additional information")
    @JsonProperty("additional_info")
    private String additionalInfo;

    @JsonProperty("match_ticket_quantity_with_attendees")
    private Boolean matchTicketQuantityWithAttendees;

    @Schema(description = "Whether the event is published/visible")
    @JsonProperty("is_publish")
    private Boolean isPublish;

    @Schema(description = "Minimum activity duration threshold (hours)")
    @JsonProperty("min_activity_threshold_time")
    private Double minActivityThresholdTime;

    @Schema(description = "Maximum activity duration threshold (hours)")
    @JsonProperty("max_activity_threshold_time")
    private Double maxActivityThresholdTime;

    @Schema(description = "Status of the creation")
    @JsonProperty("status")
    private Enums.EventStatus status;

    @Schema(description = "When the event was created")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Schema(description = "When the event was updated")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @Schema(description = "Who created the event")
    @JsonProperty("created_by")
    private Long createdBy;

    @Schema(description = "Who updated the event")
    @JsonProperty("updated_by")
    private Long updatedBy;

    @Schema(description = "Message confirming success", example = "Event created successfully")
    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OccupancyDTO {
        @JsonProperty("event_date")
        private LocalDate eventDate;
        @JsonProperty("event_time")
        private String eventTime;
//        @JsonProperty("max_capacity")
//        private int maxCapacity;
        @JsonProperty("total_booked")
        private int totalBooked;
        @JsonProperty("booking_percentage")
        private BigDecimal bookingPercentage;
        @JsonProperty("total_checked_in")
        private int totalCheckedIn;
        @JsonProperty("status")
        private Enums.OccupancyStatus status;
    }
}
