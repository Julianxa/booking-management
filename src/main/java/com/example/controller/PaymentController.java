package com.example.controller;

import com.example.model.dto.GetPaymentDetailsRequestDTO;
import com.example.model.dto.GetPaymentDetailsResponseDTO;
import com.example.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments", description = "Payment management APIs")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/details")
    public ResponseEntity<?> getPaymentDetails(
            @RequestBody GetPaymentDetailsRequestDTO request) {

        if (StringUtils.isBlank(request.getSessionId())) {
            return ResponseEntity.badRequest()
                    .body(GetPaymentDetailsResponseDTO.error("Session ID is required"));
        }

        GetPaymentDetailsResponseDTO response = paymentService.getPaymentDetails(request.getSessionId());
        return ResponseEntity.ok(response);
    }
}
