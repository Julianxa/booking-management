package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
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
}
