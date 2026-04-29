package com.example.model.dto;


import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateBookingResponseDTO {
    @Schema(description = "Booking Event reference number")
    private String bookingEventId;

    @Schema(description = "Updated list of attendees")
    private List<CreateBookingRequestDTO.AttendeeDTO> attendees;

    @Schema(description = "notes")
    private String notes;

    @Schema(description = "Success message")
    private String message;

    @Schema(description = "Timestamp of update")
    private LocalDateTime timestamp;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

}
