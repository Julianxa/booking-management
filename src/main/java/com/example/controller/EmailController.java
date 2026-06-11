package com.example.controller;


import com.example.model.dto.*;
import com.example.repository.EmailTemplatesRepository;
import com.example.service.BookingService;
import com.example.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Emails", description = "Email management APIs")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class EmailController {
    private final EmailService emailService;
    private final BookingService bookingService;
    private final EmailTemplatesRepository emailTemplatesRepository;

    @Operation(summary = "Create an email template")
    @PostMapping("/emails/template")
    public ResponseEntity<?> createTemplate(
            @Valid @RequestBody CreateEmailTemplatesRequestDTO dto) {
        CreateEmailTemplatesResponseDTO response = emailService.createEmailTemplate(dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an email template by template ID")
    @PutMapping("/emails/template/{id}")
    public ResponseEntity<?> updateTemplate(
            @PathVariable String id,
            @Valid @RequestBody CreateEmailTemplatesRequestDTO dto) {
        CreateEmailTemplatesResponseDTO response = emailService.updateEmailTemplate(id, dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete an email template by template ID")
    @DeleteMapping("/emails/template/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<?> deleteTemplate(@PathVariable String id) {
        DeleteEmailTemplateResponseDTO deleteEmailTemplateResponseDTO = emailService.deleteEmailTemplate(id);
        return ResponseEntity.ok(deleteEmailTemplateResponseDTO);
    }

    @Operation(summary = "Resend an email by bookingId")
    @PostMapping("/emails/booking/{bookingId}/resend")
    public ResponseEntity<?> resendEmail(@PathVariable String bookingId,
                                         @RequestBody ResendEmailRequestDTO dto) {
        ResendEmailResponseDTO response = bookingService.resendEmail(bookingId, dto.getEmailType());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get an email template by ID",
            description = "Retrieves an email template by ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "An email template retrieved successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = GetListEmailTemplatesResponseDTO.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Email template not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
                    )
            }
    )
    @GetMapping("/emails/template/{id}")
    public ResponseEntity<?> getEmailTemplate(@PathVariable String id) {
        GetEmailTemplateResponseDTO emailTemplate = emailService.getEmailTemplate(id);
        return ResponseEntity.ok(emailTemplate);
    }

    @Operation(
            summary = "Get all email templates",
            description = "Retrieves a list of all email templates.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of templates retrieved successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = GetListEmailTemplatesResponseDTO.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Email templates not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
                    )
            }
    )
    @GetMapping("/emails/template")
    public ResponseEntity<?> getAllEmailTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        GetListEmailTemplatesResponseDTO emailTemplates = emailService.getAllEmailTemplates(pageable);
        return ResponseEntity.ok(emailTemplates);
    }
}