package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateBookingRequestDTO {
    @NotEmpty(message = "At least one booking event must be made")
    @Valid
    @JsonProperty("booking_events")
    private List<BookingEventDTO> bookingEvents;

    @JsonProperty("promo_code")
    private String promoCode;

    @Data
    @Builder
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookingEventDTO {
        @JsonProperty("id")
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private String id;  // refNo

        @NotEmpty(message = "At least one attendee is required")
        @Valid
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private List<AttendeeDTO> attendees;

        @NotEmpty(message = "At least one ticket type is required")
        @Valid
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private List<TicketTypeDTO> tickets;

        @JsonProperty("event")
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Event is required")
        @Valid
        private EventDTO event;

        @JsonProperty("user_id")
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private String userId;

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private Enums.BookingEventStatus status;

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private BigDecimal total;

        private String notes;

        @JsonProperty("qr_code_base64")
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private String qrCodeBase64;

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EventDTO {
        @JsonProperty("id")
        private String id;  // refNo

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("name")
        private String name;

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Date of the event",
                example = "2026-07-01"
        )
        @NotNull(message = "Event date is required")
        @FutureOrPresent(message = "Event date must be today or in the future")
        @JsonProperty("event_date")
        private LocalDate eventDate;

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Time of the event",
                example = "17:00"
        )
        @NotBlank(message = "Event time is required")
        @JsonProperty("event_time")
        private String eventTime;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TicketTypeDTO {
        @NotBlank(message = "Ticket Type ID is required")
        @JsonProperty("id")
        private String id;

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private String name;

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private String description;

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private Enums.TicketTypeStatus status;

        @Schema(description = "Number of ticket type",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than 0")
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AttendeeDTO {
        @NotNull
        @JsonProperty("first_name")
        private String firstName;
        @NotNull
        @JsonProperty("last_name")
        private String lastName;
        @NotNull
        @jakarta.validation.constraints.Email
        private String email;
        private String phone;
        private Character gender;
        private String country;
        private int sequence;
    }
}
