package com.example.controller;

import com.example.model.dto.*;
import com.example.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Payments", description = "Payment management APIs")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/payments/details")
    public ResponseEntity<?> getPaymentDetails(
            @RequestBody GetPaymentDetailsRequestDTO request) {

        if (StringUtils.isBlank(request.getSessionId())) {
            return ResponseEntity.badRequest()
                    .body(GetPaymentDetailsResponseDTO.error("Session ID is required"));
        }

        GetPaymentDetailsResponseDTO response = paymentService.getPaymentDetails(request.getSessionId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/refund")
    public ResponseEntity<?> refund(
            @RequestBody RefundRequestDTO request) {

        try {
            RefundResponseDTO response = paymentService.refundBooking(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        }
    }
}
