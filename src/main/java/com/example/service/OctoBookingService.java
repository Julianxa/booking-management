package com.example.service;

import com.example.config.AppProperties;
import com.example.constant.Enums;
import com.example.exception.BusinessException;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.BookingEvents;
import com.example.model.entity.BookingItems;
import com.example.model.entity.Bookings;
import com.example.model.entity.Events;
import com.example.utils.OctoAvailabilityIdCodec;
import com.example.exception.octo.OctoException;
import com.example.model.entity.OctoBookingMappings;
import com.example.model.dto.OctoDTO;
import com.example.repository.OctoBookingMappingsRepository;
import com.example.repository.BookingAttendeesRepository;
import com.example.repository.BookingEventsRepository;
import com.example.repository.BookingItemsRepository;
import com.example.repository.BookingsRepository;
import com.example.repository.TicketTypesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OctoBookingService {

    private static final DateTimeFormatter UTC_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final AppProperties appProperties;
    private final OctoCatalogService octoCatalogService;
    private final BookingService bookingService;
    private final BookingCancellationService bookingCancellationService;
    private final BookingsRepository bookingsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingItemsRepository bookingItemsRepository;
    private final BookingAttendeesRepository bookingAttendeesRepository;
    private final TicketTypesRepository ticketTypesRepository;
    private final OctoBookingMappingsRepository octoBookingMappingsRepository;

    @Transactional
    public OctoDTO.Booking reserve(OctoDTO.BookingReservationRequest request) {
        if (request.getAvailabilityId() == null || request.getAvailabilityId().isBlank()) {
            throw OctoException.badRequest("INVALID_AVAILABILITY_ID", "availabilityId is required");
        }
        if (request.getUnitItems() == null || request.getUnitItems().isEmpty()) {
            throw OctoException.badRequest("INVALID_UNIT_ITEMS", "unitItems are required");
        }

        OctoAvailabilityIdCodec.AvailabilityKey key =
                OctoAvailabilityIdCodec.decode(request.getAvailabilityId());
        String productId =
                request.getProductId() != null && !request.getProductId().isBlank()
                        ? request.getProductId()
                        : key.productId();
        if (!productId.equals(key.productId())) {
            throw OctoException.badRequest(
                    "INVALID_PRODUCT_ID", "productId does not match availabilityId");
        }
        octoCatalogService.assertOptionId(
                request.getOptionId() != null ? request.getOptionId() : key.optionId());
        Events event = octoCatalogService.requirePublishedEvent(productId);

        String uuid =
                request.getUuid() != null && !request.getUuid().isBlank()
                        ? request.getUuid()
                        : UUID.randomUUID().toString();
        if (octoBookingMappingsRepository.findByOctoUuid(uuid).isPresent()) {
            throw OctoException.conflict("DUPLICATE_BOOKING", "Booking uuid already exists: " + uuid);
        }

        CreateBookingRequestDTO createRequest = toCreateBookingRequest(request, key, productId);

        Bookings booking;
        try {
            booking = bookingService.createExternalHold(createRequest);
        } catch (BusinessException e) {
            throw OctoException.badRequest(
                    e.getCode() != null ? e.getCode() : "BAD_REQUEST", e.getMessage());
        } catch (RuntimeException e) {
            throw OctoException.badRequest("RESERVATION_FAILED", e.getMessage());
        }

        ZonedDateTime holdExpires =
                ZonedDateTime.now().plusMinutes(appProperties.getOcto().getHoldTimeoutMinutes());

        String optionId =
                request.getOptionId() != null && !request.getOptionId().isBlank()
                        ? request.getOptionId()
                        : appProperties.getOcto().getDefaultOptionId();

        OctoBookingMappings mapping =
                OctoBookingMappings.builder()
                        .octoUuid(uuid)
                        .bookingId(booking.getId())
                        .bookingRefNo(booking.getRefNo())
                        .productId(productId)
                        .optionId(optionId)
                        .availabilityId(request.getAvailabilityId())
                        .resellerReference(request.getResellerReference())
                        .octoStatus("ON_HOLD")
                        .holdExpiresAt(holdExpires)
                        .build();
        octoBookingMappingsRepository.save(mapping);

        return toOctoBooking(mapping, booking, event, request.getHolder(), request.getNotes());
    }

    @Transactional
    public OctoDTO.Booking confirm(String uuid, OctoDTO.BookingConfirmRequest request) {
        OctoBookingMappings mapping = requireMapping(uuid);
        Bookings booking = requireBooking(mapping.getBookingId());

        if ("CONFIRMED".equals(mapping.getOctoStatus())) {
            return toOctoBooking(mapping, booking, null, null, null);
        }
        if (!"ON_HOLD".equals(mapping.getOctoStatus())) {
            throw OctoException.badRequest(
                    "INVALID_BOOKING_STATUS",
                    "Booking cannot be confirmed from status " + mapping.getOctoStatus());
        }
        if (mapping.getHoldExpiresAt() != null
                && mapping.getHoldExpiresAt().isBefore(ZonedDateTime.now())) {
            bookingService.releaseExternalHold(booking);
            mapping.setOctoStatus("EXPIRED");
            octoBookingMappingsRepository.save(mapping);
            throw OctoException.badRequest("BOOKING_EXPIRED", "Reservation hold has expired");
        }

        if (request != null && request.getResellerReference() != null) {
            mapping.setResellerReference(request.getResellerReference());
        }

        try {
            bookingService.confirmExternalHold(booking);
        } catch (RuntimeException e) {
            throw OctoException.badRequest("CONFIRMATION_FAILED", e.getMessage());
        }

        booking = requireBooking(mapping.getBookingId());
        mapping.setOctoStatus("CONFIRMED");
        mapping.setConfirmedAt(ZonedDateTime.now());
        mapping.setHoldExpiresAt(null);
        octoBookingMappingsRepository.save(mapping);

        return toOctoBooking(mapping, booking, null, null, null);
    }

    @Transactional
    public OctoDTO.Booking cancel(String uuid, OctoDTO.BookingCancelRequest request) {
        OctoBookingMappings mapping = requireMapping(uuid);
        Bookings booking = requireBooking(mapping.getBookingId());

        if ("CANCELLED".equals(mapping.getOctoStatus()) || "EXPIRED".equals(mapping.getOctoStatus())) {
            return toOctoBooking(mapping, booking, null, null, null);
        }

        if ("ON_HOLD".equals(mapping.getOctoStatus())) {
            bookingService.releaseExternalHold(booking);
            mapping.setOctoStatus("CANCELLED");
            mapping.setHoldExpiresAt(null);
            octoBookingMappingsRepository.save(mapping);
            booking = requireBooking(mapping.getBookingId());
            return toOctoBooking(mapping, booking, null, null, null);
        }

        if ("CONFIRMED".equals(mapping.getOctoStatus())) {
            bookingCancellationService.cancelActiveBookingEvents(booking, true);
            booking.setStatus(Enums.BookingStatus.CANCELLED);
            bookingsRepository.save(booking);

            mapping.setOctoStatus("CANCELLED");
            octoBookingMappingsRepository.save(mapping);
            return toOctoBooking(mapping, booking, null, null, null);
        }

        throw OctoException.badRequest(
                "INVALID_BOOKING_STATUS",
                "Cannot cancel booking in status " + mapping.getOctoStatus());
    }

    @Transactional(readOnly = true)
    public OctoDTO.Booking getBooking(String uuid) {
        OctoBookingMappings mapping = requireMapping(uuid);
        Bookings booking = requireBooking(mapping.getBookingId());
        return toOctoBooking(mapping, booking, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<OctoDTO.Booking> getBookings(
            String resellerReference,
            String supplierReference,
            String localDateStart,
            String localDateEnd) {
        ZoneId zone = ZoneId.of(appProperties.getOcto().getTimeZone());
        ZonedDateTime start =
                localDateStart != null && !localDateStart.isBlank()
                        ? java.time.LocalDate.parse(localDateStart).atStartOfDay(zone)
                        : null;
        ZonedDateTime end =
                localDateEnd != null && !localDateEnd.isBlank()
                        ? java.time.LocalDate.parse(localDateEnd).plusDays(1).atStartOfDay(zone)
                        : null;

        List<OctoDTO.Booking> result = new ArrayList<>();
        for (OctoBookingMappings mapping :
                octoBookingMappingsRepository.findFiltered(
                        blankToNull(resellerReference), blankToNull(supplierReference), start, end)) {
            bookingsRepository
                    .findById(mapping.getBookingId())
                    .ifPresent(booking -> result.add(toOctoBooking(mapping, booking, null, null, null)));
        }
        return result;
    }

    private CreateBookingRequestDTO toCreateBookingRequest(
            OctoDTO.BookingReservationRequest request,
            OctoAvailabilityIdCodec.AvailabilityKey key,
            String productId) {

        Map<String, Integer> qtyByUnit = new HashMap<>();
        List<CreateBookingRequestDTO.AttendeeDTO> attendees = new ArrayList<>();
        int sequence = 1;
        for (OctoDTO.UnitItemRequest unitItem : request.getUnitItems()) {
            if (unitItem.getUnitId() == null || unitItem.getUnitId().isBlank()) {
                throw OctoException.badRequest("INVALID_UNIT_ID", "unitId is required on unitItems");
            }
            qtyByUnit.merge(unitItem.getUnitId(), 1, Integer::sum);
            OctoDTO.Contact contact =
                    unitItem.getContact() != null ? unitItem.getContact() : request.getHolder();
            attendees.add(toAttendee(contact, sequence++));
        }

        List<CreateBookingRequestDTO.TicketTypeDTO> tickets =
                qtyByUnit.entrySet().stream()
                        .map(
                                e ->
                                        CreateBookingRequestDTO.TicketTypeDTO.builder()
                                                .id(e.getKey())
                                                .quantity(e.getValue())
                                                .build())
                        .toList();

        String timeForBooking = key.time();
        if (timeForBooking != null && timeForBooking.length() > 5) {
            timeForBooking = timeForBooking.substring(0, 5);
        }

        CreateBookingRequestDTO.EventDTO eventDTO =
                CreateBookingRequestDTO.EventDTO.builder()
                        .id(productId)
                        .eventDate(key.date())
                        .eventTime(timeForBooking)
                        .build();

        CreateBookingRequestDTO.BookingEventDTO bookingEventDTO =
                CreateBookingRequestDTO.BookingEventDTO.builder()
                        .event(eventDTO)
                        .tickets(tickets)
                        .attendees(attendees)
                        .notes(request.getNotes())
                        .answer("N/A")
                        .build();

        return CreateBookingRequestDTO.builder()
                .bookingEvents(List.of(bookingEventDTO))
                .language(Enums.Language.EN)
                .build();
    }

    private CreateBookingRequestDTO.AttendeeDTO toAttendee(OctoDTO.Contact contact, int sequence) {
        String fullName =
                contact != null && contact.getName() != null ? contact.getName().trim() : "Guest";
        String firstName = fullName;
        String lastName = "-";
        int space = fullName.indexOf(' ');
        if (space > 0) {
            firstName = fullName.substring(0, space);
            lastName = fullName.substring(space + 1).trim();
            if (lastName.isBlank()) {
                lastName = "-";
            }
        }
        String email =
                contact != null
                                && contact.getEmailAddress() != null
                                && !contact.getEmailAddress().isBlank()
                        ? contact.getEmailAddress()
                        : "klook-noreply@example.com";
        return CreateBookingRequestDTO.AttendeeDTO.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(contact != null ? contact.getPhoneNumber() : null)
                .country(contact != null ? contact.getCountry() : null)
                .sequence(sequence)
                .build();
    }

    private OctoDTO.Booking toOctoBooking(
            OctoBookingMappings mapping,
            Bookings booking,
            Events eventHint,
            OctoDTO.Contact holderHint,
            String notesHint) {

        List<BookingEvents> bookingEvents = bookingEventsRepository.findByBookingId(booking.getId());
        BookingEvents primary = bookingEvents.isEmpty() ? null : bookingEvents.get(0);

        List<OctoDTO.UnitItem> unitItems = new ArrayList<>();
        List<OctoDTO.DeliveryOption> deliveryOptions = new ArrayList<>();
        OctoDTO.Contact contact = holderHint;

        if (primary != null) {
            List<BookingItems> items = bookingItemsRepository.findByBookingEventId(primary.getId());
            List<CreateBookingRequestDTO.AttendeeDTO> attendees =
                    bookingAttendeesRepository.findAttendeesByBookingEventId(primary.getId());
            String token = primary.getVerificationToken();

            int idx = 0;
            for (BookingItems item : items) {
                String unitId =
                        ticketTypesRepository.findRefNoById(item.getTicketTypeId()).orElse(null);
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                for (int i = 0; i < qty; i++) {
                    CreateBookingRequestDTO.AttendeeDTO attendee =
                            idx < attendees.size() ? attendees.get(idx) : null;
                    idx++;
                    unitItems.add(
                            OctoDTO.UnitItem.builder()
                                    .uuid(
                                            UUID.nameUUIDFromBytes(
                                                            (mapping.getOctoUuid()
                                                                            + "-"
                                                                            + item.getId()
                                                                            + "-"
                                                                            + i)
                                                                    .getBytes())
                                                    .toString())
                                    .unitId(unitId)
                                    .status(mapUnitStatus(mapping.getOctoStatus()))
                                    .contact(attendeeDtoToContact(attendee))
                                    .ticket(
                                            OctoDTO.Ticket.builder()
                                                    .redemptionMethod("DIGITAL")
                                                    .deliveryOptions(
                                                            List.of(
                                                                    OctoDTO.DeliveryOption.builder()
                                                                            .deliveryFormat("QRCODE")
                                                                            .deliveryValue(token)
                                                                            .build()))
                                                    .build())
                                    .build());
                }
            }
            if (token != null) {
                deliveryOptions.add(
                        OctoDTO.DeliveryOption.builder()
                                .deliveryFormat("QRCODE")
                                .deliveryValue(token)
                                .build());
            }
            if (contact == null && !attendees.isEmpty()) {
                contact = attendeeDtoToContact(attendees.get(0));
            }
        }

        OctoAvailabilityIdCodec.AvailabilityKey key =
                OctoAvailabilityIdCodec.decode(mapping.getAvailabilityId());
        Events event =
                eventHint != null ? eventHint : (primary != null ? primary.getEvent() : null);
        Integer duration = event != null ? event.getDuration() : 60;

        boolean confirmed = "CONFIRMED".equals(mapping.getOctoStatus());
        boolean onHold = "ON_HOLD".equals(mapping.getOctoStatus());

        return OctoDTO.Booking.builder()
                .id(mapping.getOctoUuid())
                .uuid(mapping.getOctoUuid())
                .testMode(false)
                .resellerReference(mapping.getResellerReference())
                .supplierReference(booking.getRefNo())
                .status(mapping.getOctoStatus())
                .utcExpiresAt(formatUtc(mapping.getHoldExpiresAt()))
                .utcConfirmedAt(formatUtc(mapping.getConfirmedAt()))
                .cancellable(onHold || confirmed)
                .freeCancellationAvailable(onHold)
                .productId(mapping.getProductId())
                .optionId(mapping.getOptionId())
                .availability(
                        OctoDTO.Availability.builder()
                                .id(mapping.getAvailabilityId())
                                .localDateTimeStart(
                                        OctoAvailabilityIdCodec.toOctoLocalDateTime(
                                                key.date(), key.time()))
                                .localDateTimeEnd(
                                        OctoAvailabilityIdCodec.toOctoLocalDateTimeEnd(
                                                key.date(), key.time(), duration))
                                .allDay(false)
                                .build())
                .contact(contact)
                .unitItems(unitItems)
                .deliveryOptions(confirmed ? deliveryOptions : null)
                .voucher(
                        confirmed && primary != null ? primary.getVerificationToken() : null)
                .pricing(octoCatalogService.toPricing(booking.getFinalPaidAmount()))
                .notes(notesHint)
                .build();
    }

    private static String mapUnitStatus(String octoStatus) {
        return switch (octoStatus) {
            case "CONFIRMED" -> "CONFIRMED";
            case "CANCELLED", "EXPIRED" -> "CANCELLED";
            default -> "ON_HOLD";
        };
    }

    private static OctoDTO.Contact attendeeDtoToContact(CreateBookingRequestDTO.AttendeeDTO attendee) {
        if (attendee == null) {
            return null;
        }
        String name =
                ((attendee.getFirstName() != null ? attendee.getFirstName() : "")
                                + " "
                                + (attendee.getLastName() != null ? attendee.getLastName() : ""))
                        .trim();
        return OctoDTO.Contact.builder()
                .name(name)
                .emailAddress(attendee.getEmail())
                .phoneNumber(attendee.getPhone())
                .country(attendee.getCountry())
                .build();
    }

    private OctoBookingMappings requireMapping(String uuid) {
        return octoBookingMappingsRepository
                .findByOctoUuid(uuid)
                .orElseThrow(
                        () ->
                                OctoException.notFound(
                                        "INVALID_BOOKING_UUID", "Booking not found: " + uuid));
    }

    private Bookings requireBooking(Long bookingId) {
        return bookingsRepository
                .findById(bookingId)
                .orElseThrow(
                        () ->
                                OctoException.notFound(
                                        "INVALID_BOOKING_UUID",
                                        "Booking not found for id " + bookingId));
    }

    private static String formatUtc(ZonedDateTime time) {
        if (time == null) {
            return null;
        }
        return time.withZoneSameInstant(ZoneOffset.UTC).format(UTC_FMT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
