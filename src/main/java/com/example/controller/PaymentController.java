package com.example.controller;

import com.example.model.dto.*;
import com.example.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
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

    @Operation(
            summary = "Retrieve payment details by session ID",
            description = "Retrieve payment details by session ID after payment.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Retrieve payment details successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetPaymentDetailsResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
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

    @Operation(
            summary = "Refund a payment online/offline",
            description = "Online full refund or Offline partial refund.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Refund the payment successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RefundResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/payments/refund")
    public ResponseEntity<?> refund(
            @RequestBody RefundRequestDTO request) {
        RefundResponseDTO response = paymentService.refundBooking(request);
        return ResponseEntity.ok(response);
    }
}
