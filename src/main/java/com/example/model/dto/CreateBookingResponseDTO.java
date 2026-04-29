package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateBookingResponseDTO {
    private String id;
    @Schema(description = "List of all bookings created in this operation")
    @JsonProperty("booking_events")
    private List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents;
    @Schema(description = "Grand total amount paid")
    @JsonProperty("total_paid_amount")
    private BigDecimal totalPaidAmount;
    @JsonProperty("discount")
    private BigDecimal discount;
    @JsonProperty("final_paid_amount")
    private BigDecimal finalPaidAmount;
    private Enums.BookingStatus status;
    @JsonProperty("promo_code")
    private String promoCode;
    @JsonProperty("redeemed_at")
    private LocalDateTime redeemedAt;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
