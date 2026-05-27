package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundResponseDTO {
    @JsonProperty("id")
    private String id;

    @JsonProperty("booking_id")
    private String bookingId;

    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;

    @JsonProperty("refund_currency")
    private String refundCurrency;

    @JsonProperty("refund_type")
    private Enums.RefundType refundType;

    @JsonProperty("status")
    private Enums.RefundStatus status;

    @JsonProperty("remarks")
    private String remarks;

    @Schema(description = "Message confirming success", example = "Refund created successfully")
    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
