package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundRequestDTO {
    @JsonProperty("booking_id")
    private String bookingId;
    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;
    @JsonProperty("refund_currency")
    private String refundCurrency;
    @JsonProperty("is_full_refund")
    private Boolean isFullRefund;
}
