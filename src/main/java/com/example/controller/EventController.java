package com.example.controller;

import com.example.exception.ResourceNotFoundException;
import com.example.model.dto.*;
import com.example.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Events", description = "Event management APIs")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class EventController {
    private final EventService eventService;

    @Operation(
            summary = "Create a new event",
            description = "Creates a new event record.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Event created successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateEventResponseDTO.class))),
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
    @PostMapping(value="/events", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart(value = "eventPic", required = false) MultipartFile eventPic,
            @RequestPart(value = "contactInfo", required = true)
            @Schema(
                    description = "Event information in JSON format",
                    type = "string",
                    implementation = CreateEventRequestDTO.class,
                    format = "textarea",
                    required = true) String createEventRequestDTOJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            CreateEventRequestDTO createEventRequestDTO = mapper.readValue(createEventRequestDTOJson, CreateEventRequestDTO.class);
            return ResponseEntity.ok(eventService.createEvent(createEventRequestDTO, eventPic));
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
            summary = "Get event by ID",
            description = "Retrieves details of a specific event.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Event found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateEventResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Event not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/events/{id}")
    public ResponseEntity<?> getEvent(@PathVariable String id) {
        try {
            CreateEventResponseDTO createEventResponseDTO = eventService.getEvent(id);
            return ResponseEntity.ok(createEventResponseDTO);
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
            summary = "Get event's availability",
            description = "Returns availability of a event (changes when booking is made).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of events",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetListEventAvailabilityResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/events/{id}/availability")
    public ResponseEntity<?> getEventAvailability(@PathVariable String id,
                                                @RequestParam(required = false) String search,
                                                @Parameter(
                                                        description = "Filter events by a specific date",
                                                        example = "2026-03-24",
                                                        schema = @Schema(type = "string", format = "date")
                                                )
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            EventAvailabilityDTO events = eventService.getAvailability(id, date);

            return ResponseEntity.ok(events);
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
            summary = "List all events' availability",
            description = "Returns availabilities of all events (changes when bookings are made).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of events",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetListEventAvailabilityResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/events/availability")
    public ResponseEntity<?> getAllAvailability(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
                                    @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,
                                    @RequestParam(required = false) String search,
                                    @Parameter(
                                            description = "Filter events by a specific date",
                                            example = "2026-03-24",
                                            schema = @Schema(type = "string", format = "date")
                                    )
                                    @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            GetListEventAvailabilityResponseDTO events = eventService.getAllAvailabilities(pageable, search, date);

            return ResponseEntity.ok(events);
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
            summary = "List all events",
            description = "Returns a list of all events (unchanged by bookings).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of events",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetListEventResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/events")
    public ResponseEntity<?> getAll(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
                                    @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,
                                    @RequestParam(required = false) String search) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            GetListEventResponseDTO events = eventService.getAllEvents(pageable, search);

            return ResponseEntity.ok(events);
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
            summary = "Update an event",
            description = "Updates an existing event (partial update).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Event updated successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateEventResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Event not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PutMapping(value="/events/{id}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@RequestPart(value = "eventPic", required = false) MultipartFile eventPic,
                                    @PathVariable String id,
                                    @Schema(
                                            description = "Event information in JSON format",
                                            type = "string",
                                            implementation = UpdateEventRequestDTO.class,
                                            format = "textarea",
                                            required = true) String updateEventRequestDTOJson
                                    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            UpdateEventRequestDTO updateEventRequestDTO = mapper.readValue(updateEventRequestDTOJson, UpdateEventRequestDTO.class);

            UpdateEventResponseDTO updateEventResponseDTO = eventService.updateEvent(id, updateEventRequestDTO, eventPic);
            return ResponseEntity.ok(updateEventResponseDTO);
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
            summary = "Update the status of an event",
            description = "Update the status of an event by ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Event status updated successfully",
                                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateEventStatusResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Event not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PutMapping("/events/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> updateEventStatus(@PathVariable String id,
                                               @RequestBody UpdateEventStatusRequestDTO request) {
        try {
            UpdateEventStatusResponseDTO updateEventStatusResponseDTO = eventService.updateEventStatus(id, request);
            return ResponseEntity.ok(updateEventStatusResponseDTO);
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
            summary = "Create a new ticket type for an event",
            description = "Adds a new ticket type (e.g. Adult, Student, VIP) to an existing event. " +
                    "The event must already exist.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Ticket type created successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateTicketTypeResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data or event not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Event not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
                    )
            }
    )
    @PostMapping("/events/{id}/ticket-types")
    public ResponseEntity<?> createTicketType(
            @PathVariable("id") String id,
            @RequestBody CreateTicketTypeRequestDTO dto) {

        try {
            CreateTicketTypeResponseDTO response = eventService.createTicketType(id, dto);
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
            summary = "Update an existing ticket type",
            description = "Partially updates a ticket type (e.g. change price, capacity, status). " +
                    "Only provided fields are updated.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Ticket type updated successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateTicketTypeResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input or business rule violation",
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
                            description = "Event or ticket type not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
                    )
            }
    )
    @PutMapping("/events/{id}/ticket-types/{ticketTypeId}")
    public ResponseEntity<?> updateTicketType(
            @PathVariable String id,
            @PathVariable String ticketTypeId,
            @Valid @RequestBody UpdateTicketTypeRequestDTO dto) {

        try {
            UpdateTicketTypeResponseDTO response = eventService.updateTicketType(id, ticketTypeId, dto);
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
            summary = "Update the status of a ticket type",
            description = "Update the status of a ticket type from an event. " +
                    "The ticket type must belong to the specified event. ",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Status of ticket type updated successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateTicketTypeStatusResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request or business rule violation (e.g. ticket type in use)",
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
                            description = "Event or ticket type not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
                    )
            }
    )
    @PutMapping("/events/{id}/ticket-types/{ticketTypeId}/status")
    public ResponseEntity<?> updateTicketTypeStatus(
            @PathVariable String id,
            @PathVariable String ticketTypeId,
            @RequestBody UpdateTicketTypeStatusRequestDTO request) {
        try {
            UpdateTicketTypeStatusResponseDTO updateTicketTypeStatusResponseDTO = eventService.updateTicketTypeStatus(id, ticketTypeId, request);
            return ResponseEntity.ok(updateTicketTypeStatusResponseDTO);
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
            summary = "Get all ticket types for an event",
            description = "Retrieves a list of all active (or all, depending on filters) ticket types associated with the specified event.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of ticket types retrieved successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = CreateTicketTypeResponseDTO.class))
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
                            description = "Event not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
                    )
            }
    )
    @GetMapping("/events/{id}/ticket-types")
    public ResponseEntity<?> getTicketTypesByEvent(@PathVariable String id) {

        try {
            List<CreateTicketTypeResponseDTO> ticketTypes = eventService.getTicketTypesByEventId(id);
            return ResponseEntity.ok(ticketTypes);
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
            summary = "Initiate Check-in",
            description = "Initiate the check-in when a user scans the QR code and visits the link from the frontend.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Check-in initiated successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = InitiateCheckinResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid token or ticket already checked in",
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
                            description = "Ticket or event not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
                    )
            }
    )
    @GetMapping("/events/checkin")
    public ResponseEntity<?> checkin(@RequestParam("token") String token) {
        try {
            InitiateCheckinResponseDTO initiateCheckinResponseDTO = eventService.initiateCheckin(token);
            return ResponseEntity.ok(initiateCheckinResponseDTO);
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
            summary = "Confirm Check-in",
            description = "Confirms and completes the check-in after the frontend has scanned the QR code " +
                    "and retrieved ticket information in the first step.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Check-in confirmed successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ConfirmCheckinResponseDTO.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid token or already checked in"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @PostMapping("/events/confirm")
    public ResponseEntity<?> confirmCheckin(@RequestBody ConfirmCheckinRequestDTO request) {
        try {
            ConfirmCheckinResponseDTO response = eventService.confirmCheckin(request);
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
}
