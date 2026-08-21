package com.example.controller;

import com.example.model.dto.*;
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
import java.time.ZonedDateTime;

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

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(bookingService.getPassengersByEventDateTime(eventId, eventDate, eventTime, pageable));
    }

    @Operation(
            summary = "List all bookings by event ID",
            description = "Returns bookings for an event. Optionally filter by eventDate and/or eventTime.",
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(required = false) String eventTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(bookingService.getEventBookings(eventId, eventDate, eventTime, pageable));
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

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(bookingService.getUserBookings(userId, pageable));
    }

    @Operation(
            summary = "Search bookings by booking_id, attendee name, email, or phone",
            description = "Search bookings by booking_id, attendee name, email, or phone. "
                    + "Use field=booking_id|name|email|phone and provide the search value.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of matching bookings",
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
    @GetMapping("/bookings/search")
    public ResponseEntity<?> searchBookingsByAttendee(
            @RequestParam("field") String field,
            @RequestParam("value") String value,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(bookingService.searchBookingsByAttendee(field, value, pageable));
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
            @Valid @RequestBody CreateBookingRequestDTO request) {

        String userSub = userUtils.extractUserSub(authorizationHeader);

        CreateBookingResponseDTO createBookingResponseDTO = bookingService.createBooking(userSub, request);

        createBookingResponseDTO.setMessage("Booking created successfully. Please complete payment.");
        createBookingResponseDTO.setTimestamp(ZonedDateTime.now());
        return ResponseEntity.status(HttpStatus.OK).body(createBookingResponseDTO);
    }

    @Operation(
            summary = "Update the status of a booked event by booking event ID",
            description = "Updates the status for a specific booking event (CHECKED_IN, AVAILABLE, NO_SHOW, CANCELLED). ",
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
            @Valid @RequestBody UpdateBookingEventStatusRequestDTO request) {

        String userSub = userUtils.extractUserSub(authorizationHeader);

        UpdateBookingEventStatusResponseDTO response = bookingService.updateBookingEventStatus(userSub, bookingEventId, request);

        return ResponseEntity.ok(response);
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
    @PatchMapping("/bookings/event/{bookingEventId}")
    public ResponseEntity<?> updateBookingAttendeesByBookingEventId(
            @PathVariable("bookingEventId") String bookingEventId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody UpdateBookingRequestDTO request) {

        String userSub = userUtils.extractUserSub(authorizationHeader);

        UpdateBookingResponseDTO response = bookingService.updateBooking(userSub, bookingEventId, request);

        return ResponseEntity.ok(response);
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
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }
}