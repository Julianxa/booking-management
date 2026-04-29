package com.example.model.dto;


import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventAvailabilityDTO {
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

    @Schema(description = "Overall maximum capacity")
    @JsonProperty("max_capacity")
    private Integer maxCapacity;

    @JsonProperty("status")
    private Enums.EventStatus status;

    @JsonProperty("occupancy")
    private List<CreateEventResponseDTO.OccupancyDTO> occupancy;

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
}
