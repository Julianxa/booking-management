package com.example.model.dto;

import com.example.constant.Enums;
import com.example.model.entity.EmailTemplates;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
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
    private Enums.BookingType type;
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
    @JsonProperty("currency")
    private String currency;
    @JsonProperty("status")
    private Enums.BookingStatus status;
    @JsonProperty("language")
    private Enums.Language language;
    @JsonProperty("email_template")
    private EmailTemplates emailTemplate;
    @JsonProperty("promo_code")
    private String promoCode;
    @JsonProperty("checkout_url")
    private String checkoutUrl;
    @JsonProperty("redeemed_at")
    private ZonedDateTime redeemedAt;
    @JsonProperty("created_at")
    private ZonedDateTime createdAt;
    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;
}
