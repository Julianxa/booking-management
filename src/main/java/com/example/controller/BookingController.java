package com.example.controller;

import com.example.exception.InvalidIdTokenException;
import com.example.exception.ResourceNotFoundException;
import com.example.model.dto.*;
import com.example.service.AwsService;
import com.example.service.BookingService;
import com.example.utils.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Tag(name = "Bookings", description = "Booking management APIs")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    private final UserUtils userUtils;

    @Operation(
            summary = "List all participants by event ID, event date and event time",
            description = "Returns a list of all participants.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of participants",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetListParticipantsResponseDTO.class))),
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
    @GetMapping("/bookings/event/{eventId}/participants")
    public ResponseEntity<?> getParticipantsListByEventId(
            @PathVariable("eventId") String eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam String eventTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            return ResponseEntity.ok(bookingService.getPassengersByEventDateTime(eventId, eventDate, eventTime, pageable));
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
            summary = "List all bookings by event ID",
            description = "Returns a list of all bookings.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of bookings",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetListBookingResponseDTO.class))),
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
    @GetMapping("/bookings/event/{eventId}")
    public ResponseEntity<?> getBookingsByEventId(
            @PathVariable("eventId") String eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            return ResponseEntity.ok(bookingService.getEventBookings(eventId, pageable));
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
            summary = "List all bookings by user ID",
            description = "Returns a list of all bookings.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of bookings",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetListBookingResponseDTO.class))),
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
    @GetMapping("/bookings/user/{userId}")
    public ResponseEntity<?> getBookingsByUserId(
            @PathVariable("userId") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            return ResponseEntity.ok(bookingService.getUserBookings(userId, pageable));
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
            summary = "Create a new booking",
            description = "Creates a new booking record.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Booking created successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateBookingResponseDTO.class))),
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
    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Access-Token", required = false) String accessToken,
            @Valid @RequestBody CreateBookingRequestDTO request) throws InvalidIdTokenException {

        try {
            String userSub = userUtils.extractUserSub(authorizationHeader);

            CreateBookingResponseDTO createBookingResponseDTO = bookingService.createBooking(userSub, request);
            return ResponseEntity.status(HttpStatus.OK).body(createBookingResponseDTO);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
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
            summary = "Update the status of a booked event by booking event ID",
            description = "Updates the status for a specific booking event. ",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Status of an event updated successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateBookingEventStatusResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Booked event not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PutMapping("/bookings/event/{bookingEventId}/status")
    public ResponseEntity<?> updateStatusByBookingEventId(
            @PathVariable("bookingEventId") String bookingEventId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody UpdateBookingEventStatusRequestDTO request) throws InvalidIdTokenException {

        String userSub = userUtils.extractUserSub(authorizationHeader);

        try {
            UpdateBookingEventStatusResponseDTO response = bookingService.updateBookingEventStatus(bookingEventId, request);

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
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
            summary = "Update a booked event by booking event ID",
            description = "Updates information for a specific booking event. ",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Booking updated successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateBookingResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Booking or event not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PutMapping("/bookings/event/{bookingEventId}")
    public ResponseEntity<?> updateBookingAttendeesByBookingEventId(
            @PathVariable("bookingEventId") String bookingEventId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody UpdateBookingRequestDTO request) throws InvalidIdTokenException {

        String userSub = userUtils.extractUserSub(authorizationHeader);

        try {
            UpdateBookingResponseDTO response = bookingService.updateBooking(bookingEventId, request);

            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
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
            summary = "Get booking by ID",
            description = "Retrieves details of a specific booking.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Event created successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateBookingResponseDTO.class))),
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
    @GetMapping("/bookings/{id}")
    public ResponseEntity<?> getBooking(@PathVariable String id) {
        try {
            return ResponseEntity.ok(bookingService.getBookingById(id));
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