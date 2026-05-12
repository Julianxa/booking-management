package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetPaymentDetailsResponseDTO {
    @JsonProperty("booking_id")
    private String bookingId;
    @JsonProperty("payment_id")
    private String paymentId;
    @JsonProperty("payment_platform")
    private Enums.PaymentPlatform paymentPlatform;
    @JsonProperty("payment_channel")
    private Enums.PaymentChannel paymentChannel;
    private BigDecimal amount;
    private String currency;
    @JsonProperty("payment_status")
    private Enums.PaymentStatus paymentStatus;
    @JsonProperty("paid_at")
    private LocalDateTime paidAt;
    private String message;

    public static GetPaymentDetailsResponseDTO error(String message) {
        GetPaymentDetailsResponseDTO res = new GetPaymentDetailsResponseDTO();
        res.message = message;
        return res;
    }
}
