package com.example.controller;


import com.example.exception.ResourceNotFoundException;
import com.example.model.dto.ErrorResponseDTO;
import com.example.model.dto.ResendConfirmationEmailResponseDTO;
import com.example.model.dto.UpdateEmailTemplatesRequestDTO;
import com.example.model.dto.UpdateEmailTemplatesResponseDTO;
import com.example.service.BookingService;
import com.example.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Tag(name = "Emails", description = "Email management APIs")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class EmailController {
    private final EmailService emailService;
    private final BookingService bookingService;

    @Operation(summary = "Update an email template by template ID")
    @PutMapping("/emails/template/{id}")
    public ResponseEntity<?> updateTemplate(
            @PathVariable String id,
            @Valid @RequestBody UpdateEmailTemplatesRequestDTO dto) {

        try {
            UpdateEmailTemplatesResponseDTO response = emailService.updateEmailTemplate(id, dto);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        }
    }

    @Operation(summary = "Resend an email by bookingEventId")
    @PostMapping("/emails/booking/{bookingEventId}/confirmation-resend")
    public ResponseEntity<?> resendConfirmationEmail(@PathVariable String bookingEventId) {

        try {
            ResendConfirmationEmailResponseDTO response = bookingService.reConfirmBooking(bookingEventId);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .message("Error resending email: " + e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        }
    }

}