package com.example.controller;

import com.example.model.dto.OctoDTO;
import com.example.service.OctoAvailabilityService;
import com.example.service.OctoBookingService;
import com.example.service.OctoCatalogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@Tag(name = "OCTO / Klook", description = "Supplier OCTO endpoints called by Klook")
@SecurityRequirement(name = "OctoApiKey")
@RestController
@RequestMapping("/octo")
@RequiredArgsConstructor
public class OctoController {

    private final OctoCatalogService octoCatalogService;
    private final OctoAvailabilityService octoAvailabilityService;
    private final OctoBookingService octoBookingService;

    @GetMapping("/supplier")
    public OctoDTO.Supplier getSupplier() {
        return octoCatalogService.getSupplier();
    }

    @GetMapping("/products")
    public List<OctoDTO.Product> getProducts() {
        return octoCatalogService.getProducts();
    }

    @GetMapping("/products/{id}")
    public OctoDTO.Product getProduct(@PathVariable("id") String id) {
        return octoCatalogService.getProduct(id);
    }

    @PostMapping("/availability")
    public List<OctoDTO.Availability> checkAvailability(
            @RequestBody OctoDTO.AvailabilityRequest request) {
        return octoAvailabilityService.checkAvailability(request);
    }

    @PostMapping("/availability/calendar")
    public List<OctoDTO.AvailabilityCalendarDay> availabilityCalendar(
            @RequestBody OctoDTO.AvailabilityCalendarRequest request) {
        return octoAvailabilityService.calendar(request);
    }

    @PostMapping("/bookings")
    public OctoDTO.Booking reserve(@RequestBody OctoDTO.BookingReservationRequest request) {
        return octoBookingService.reserve(request);
    }

    @PostMapping("/bookings/{uuid}/confirm")
    public OctoDTO.Booking confirm(
            @PathVariable("uuid") String uuid,
            @RequestBody(required = false) OctoDTO.BookingConfirmRequest request) {
        return octoBookingService.confirm(uuid, request);
    }

    @PostMapping("/bookings/{uuid}/cancel")
    public OctoDTO.Booking cancel(
            @PathVariable("uuid") String uuid,
            @RequestBody(required = false) OctoDTO.BookingCancelRequest request) {
        return octoBookingService.cancel(uuid, request);
    }

    @GetMapping("/bookings/{uuid}")
    public OctoDTO.Booking getBooking(@PathVariable("uuid") String uuid) {
        return octoBookingService.getBooking(uuid);
    }

    @GetMapping("/bookings")
    public List<OctoDTO.Booking> getBookings(
            @RequestParam(value = "resellerReference", required = false) String resellerReference,
            @RequestParam(value = "supplierReference", required = false) String supplierReference,
            @RequestParam(value = "localDateStart", required = false) String localDateStart,
            @RequestParam(value = "localDateEnd", required = false) String localDateEnd) {
        return octoBookingService.getBookings(
                resellerReference, supplierReference, localDateStart, localDateEnd);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
