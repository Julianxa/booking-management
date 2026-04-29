package com.example.controller;

import com.example.exception.ResourceNotFoundException;
import com.example.model.dto.*;
import com.example.service.AwsService;
import com.example.service.GiftCertificateService;
import com.example.utils.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.time.LocalDateTime;

@Tag(name = "Gift Certificates", description = "Gift certificate management APIs")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class GiftCertificateController {
    private final GiftCertificateService giftCertificateService;
    private final UserUtils userUtils;

    @Operation(
            summary = "Create a new gift certificate",
            description = "Create VALUE/EVENT gift certificate. ",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Gift Certificate created successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateGiftCertificateResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/gift-certificates")
    public ResponseEntity<?> create(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Access-Token", required = false) String accessToken,
            @Valid @RequestBody CreateGiftCertificateRequestDTO createGiftCertificateRequestDTO) {
        try {
            String userSub = userUtils.extractUserSub(authorizationHeader);

            CreateGiftCertificateResponseDTO response = giftCertificateService.createCertificate(userSub, createGiftCertificateRequestDTO);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
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

    @Operation(
            summary = "Update existing gift certificate",
            description = "Update an existing gift certificate. ",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Gift Certificate updated successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateGiftCertificateResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PutMapping("/gift-certificates/{promoCode}")
    public ResponseEntity<?> updateGiftCertificate(
            @PathVariable String promoCode,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Access-Token", required = false) String accessToken,
            @Valid @RequestBody UpdateGiftCertificateRequestDTO request) {

        try {
            String userSub = userUtils.extractUserSub(authorizationHeader);
            UpdateGiftCertificateResponseDTO response = giftCertificateService.updateCertificate(promoCode, request);
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

    @Operation(
            summary = "Get gift certificate by promoCode",
            description = "Get a gift certificate. ",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Gift Certificate retrieved successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateGiftCertificateResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/gift-certificates/{promoCode}")
    public ResponseEntity<?> getCertificate(@PathVariable String promoCode) {
        try {
            return ResponseEntity.ok(giftCertificateService.getCertificate(promoCode));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        } catch(Exception e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build());
        }
    }

    @Operation(
            summary = "Get gift certificates by event ID with pagination",
            description = "Get a list of gift certificates. ",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Gift Certificate retrieved successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetListGiftCertificateResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/gift-certificates/events/{eventId}")
    public ResponseEntity<?> getGiftCertificatesByEventId(
            @Parameter(description = "Filter by event ID")
            @PathVariable String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            GetListGiftCertificateResponseDTO certificates =
                    giftCertificateService.getGiftCertificates(pageable, eventId);

            return ResponseEntity.ok(certificates);

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

    @Operation(
            summary = "Get gift certificates with pagination",
            description = "Get a list of gift certificates. ",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Gift Certificate retrieved successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetListGiftCertificateResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/gift-certificates")
    public ResponseEntity<?> getGiftCertificates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            GetListGiftCertificateResponseDTO certificates =
                    giftCertificateService.getGiftCertificates(pageable, null);

            return ResponseEntity.ok(certificates);

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

    @Operation(
            summary = "Update the status of an gift certificate",
            description = "Update the status of a gift certificate by ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Gift Certificate status updated successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateGiftCertificateStatusResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Gift Certificate not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PutMapping("/gift-certificates/{promoCode}/status")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> updateGiftCertificateStatus(@PathVariable String promoCode,
                                               @RequestBody UpdateGiftCertificateStatusRequestDTO request) {
        try {
            UpdateGiftCertificateStatusResponseDTO updateGiftCertificateStatusResponseDTO = giftCertificateService.updateGiftCertificateStatus(promoCode, request);
            return ResponseEntity.ok(updateGiftCertificateStatusResponseDTO);
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
}