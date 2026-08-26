package com.example.service;

import com.example.config.AppProperties;
import com.example.constant.Enums;
import com.example.exception.BusinessException;
import com.example.exception.octo.OctoException;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.dto.OctoDTO;
import com.example.model.entity.BookingEvents;
import com.example.model.entity.BookingItems;
import com.example.model.entity.Bookings;
import com.example.model.entity.Events;
import com.example.repository.BookingAttendeesRepository;
import com.example.repository.BookingEventsRepository;
import com.example.repository.BookingItemsRepository;
import com.example.repository.BookingsRepository;
import com.example.repository.TicketTypesRepository;
import com.example.utils.ActivityThresholdUtil;
import com.example.utils.OctoAvailabilityIdCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
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

    @Transactional
    public OctoDTO.Booking reserve(OctoDTO.BookingReservationRequest request) {
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            throw OctoException.badRequest("INVALID_PRODUCT_ID", "productId is required");
        }
        if (request.getOptionId() == null || request.getOptionId().isBlank()) {
            throw OctoException.badRequest("INVALID_OPTION_ID", "optionId is required");
        }
        if (request.getAvailabilityId() == null || request.getAvailabilityId().isBlank()) {
            throw OctoException.badRequest("INVALID_AVAILABILITY_ID", "availabilityId is required");
        }
        if (request.getUnitItems() == null || request.getUnitItems().isEmpty()) {
            throw OctoException.badRequest("INVALID_UNIT_ITEMS", "unitItems are required");
        }

        OctoAvailabilityIdCodec.AvailabilityKey key =
                OctoAvailabilityIdCodec.decode(request.getAvailabilityId());
        String productId = request.getProductId();
        if (!productId.equals(key.productId())) {
            throw OctoException.badRequest(
                    "INVALID_PRODUCT_ID", "productId does not match availabilityId");
        }
        octoCatalogService.assertOptionId(request.getOptionId());
        if (!request.getOptionId().equals(key.optionId())) {
            throw OctoException.badRequest(
                    "INVALID_OPTION_ID", "optionId does not match availabilityId");
        }
        Events event = octoCatalogService.requirePublishedEvent(productId);

        String uuid =
                request.getUuid() != null && !request.getUuid().isBlank()
                        ? request.getUuid()
                        : UUID.randomUUID().toString();
        if (bookingsRepository.findByOctoUuid(uuid).isPresent()) {
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

        booking.setOctoUuid(uuid);
        booking.setHoldExpiresAt(ZonedDateTime.now().plusMinutes(resolveHoldMinutes(request)));
        bookingsRepository.save(booking);

        return toOctoBooking(booking, event, null, request.getNotes(), null);
    }

    /** Honor expirationMinutes when set, capped by supplier default hold timeout. */
    private long resolveHoldMinutes(OctoDTO.BookingReservationRequest request) {
        long maxHold = appProperties.getOcto().getHoldTimeoutMinutes();
        Integer requested = request.getExpirationMinutes();
        if (requested == null || requested <= 0) {
            return maxHold;
        }
        return Math.min(requested.longValue(), maxHold);
    }

    @Transactional
    public OctoDTO.Booking confirm(String uuid, OctoDTO.BookingConfirmRequest request) {
        Bookings booking = requireOctoBooking(uuid);

        if (booking.getStatus() == Enums.BookingStatus.CONFIRMED) {
            return toOctoBooking(
                    booking,
                    null,
                    request != null ? request.getContact() : null,
                    null,
                    null);
        }
        if (booking.getStatus() != Enums.BookingStatus.ON_HOLD) {
            throw OctoException.badRequest(
                    "INVALID_BOOKING_STATUS",
                    "Booking cannot be confirmed from status " + booking.getStatus());
        }
        if (booking.getHoldExpiresAt() != null
                && booking.getHoldExpiresAt().isBefore(ZonedDateTime.now())) {
            bookingService.releaseExternalHold(booking);
            throw OctoException.badRequest("BOOKING_EXPIRED", "Reservation hold has expired");
        }

        if (request == null || request.getContact() == null) {
            throw OctoException.badRequest("INVALID_CONTACT", "contact is required");
        }
        if (request.getResellerReference() != null) {
            booking.setResellerReference(request.getResellerReference());
        }

        try {
            bookingService.confirmExternalHold(booking);
        } catch (RuntimeException e) {
            throw OctoException.badRequest("CONFIRMATION_FAILED", e.getMessage());
        }

        booking = requireOctoBooking(uuid);
        booking.setHoldExpiresAt(null);
        bookingsRepository.save(booking);

        return toOctoBooking(booking, null, request.getContact(), null, null);
    }

    @Transactional
    public OctoDTO.Booking cancel(String uuid, OctoDTO.BookingCancelRequest request) {
        Bookings booking = requireOctoBooking(uuid);
        String reason = request != null ? request.getReason() : null;

        if (booking.getStatus() == Enums.BookingStatus.CANCELLED
                || booking.getStatus() == Enums.BookingStatus.EXPIRED) {
            return toOctoBooking(booking, null, null, null, reason);
        }

        if (booking.getStatus() == Enums.BookingStatus.ON_HOLD) {
            bookingService.cancelExternalHold(booking);
            booking = requireOctoBooking(uuid);
            return toOctoBooking(booking, null, null, null, reason);
        }

        if (booking.getStatus() == Enums.BookingStatus.CONFIRMED) {
            bookingCancellationService.cancelActiveBookingEvents(booking, true);
            booking.setStatus(Enums.BookingStatus.CANCELLED);
            booking.setHoldExpiresAt(null);
            bookingsRepository.save(booking);
            return toOctoBooking(booking, null, null, null, reason);
        }

        throw OctoException.badRequest(
                "INVALID_BOOKING_STATUS",
                "Cannot cancel booking in status " + booking.getStatus());
    }

    @Transactional(readOnly = true)
    public OctoDTO.Booking getBooking(String uuid) {
        return toOctoBooking(requireOctoBooking(uuid), null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<OctoDTO.Booking> getBookings(
            String resellerReference,
            String supplierReference,
            String localDate,
            String localDateStart,
            String localDateEnd) {
        boolean hasReseller = resellerReference != null && !resellerReference.isBlank();
        boolean hasSupplier = supplierReference != null && !supplierReference.isBlank();
        boolean hasLocalDate = localDate != null && !localDate.isBlank();
        boolean hasRange =
                localDateStart != null
                        && !localDateStart.isBlank()
                        && localDateEnd != null
                        && !localDateEnd.isBlank();
        if (!hasReseller && !hasSupplier && !hasLocalDate && !hasRange) {
            throw OctoException.badRequest(
                    "BAD_REQUEST",
                    "either resellerReference, supplierReference, localDate or localDateStart/localDateEnd is required");
        }
        if ((localDateStart != null && !localDateStart.isBlank())
                ^ (localDateEnd != null && !localDateEnd.isBlank())) {
            throw OctoException.badRequest(
                    "INVALID_DATE_RANGE", "localDateStart and localDateEnd must both be provided");
        }

        LocalDate start;
        LocalDate end;
        if (hasLocalDate) {
            start = LocalDate.parse(localDate);
            end = start;
        } else {
            start =
                    localDateStart != null && !localDateStart.isBlank()
                            ? LocalDate.parse(localDateStart)
                            : null;
            end =
                    localDateEnd != null && !localDateEnd.isBlank()
                            ? LocalDate.parse(localDateEnd)
                            : null;
        }
        if (start != null && end != null && end.isBefore(start)) {
            throw OctoException.badRequest(
                    "INVALID_DATE_RANGE", "localDateEnd before localDateStart");
        }

        List<OctoDTO.Booking> result = new ArrayList<>();
        for (Bookings booking :
                bookingsRepository.findOctoBookingsFiltered(
                        Enums.BookingPlatform.KLOOK,
                        blankToNull(resellerReference),
                        blankToNull(supplierReference),
                        start,
                        end)) {
            result.add(toOctoBooking(booking, null, null, null, null));
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
            attendees.add(toAttendee(null, sequence++));
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
        String firstName = null;
        String lastName = null;
        if (contact != null) {
            firstName = blankToNull(contact.getFirstName());
            lastName = blankToNull(contact.getLastName());
            if (firstName == null && lastName == null) {
                String fullName = blankToNull(contact.getFullName());
                if (fullName != null) {
                    int space = fullName.indexOf(' ');
                    if (space > 0) {
                        firstName = fullName.substring(0, space);
                        lastName = fullName.substring(space + 1).trim();
                        if (lastName.isBlank()) {
                            lastName = null;
                        }
                    } else {
                        firstName = fullName;
                    }
                }
            }
        }
        if (firstName == null) {
            firstName = "Guest";
        }
        if (lastName == null || lastName.isBlank()) {
            lastName = "-";
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
            Bookings booking,
            Events eventHint,
            OctoDTO.Contact contactHint,
            String notesHint,
            String cancelReason) {

        List<BookingEvents> bookingEvents = bookingEventsRepository.findByBookingId(booking.getId());
        BookingEvents primary = bookingEvents.isEmpty() ? null : bookingEvents.get(0);

        List<OctoDTO.UnitItem> unitItems = new ArrayList<>();
        OctoDTO.Contact contact = contactHint;

        String octoStatus = toOctoStatus(booking.getStatus());
        String octoUuid = booking.getOctoUuid();
        boolean confirmed = booking.getStatus() == Enums.BookingStatus.CONFIRMED;
        boolean onHold = booking.getStatus() == Enums.BookingStatus.ON_HOLD;
        boolean cancelled = booking.getStatus() == Enums.BookingStatus.CANCELLED
                || booking.getStatus() == Enums.BookingStatus.EXPIRED;

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
                    OctoDTO.Ticket ticket = null;
                    if (token != null) {
                        ticket =
                                OctoDTO.Ticket.builder()
                                        .redemptionMethod("DIGITAL")
                                        .utcRedeemedAt(null)
                                        .deliveryOptions(
                                                List.of(
                                                        OctoDTO.DeliveryOption.builder()
                                                                .deliveryFormat("QRCODE")
                                                                .deliveryValue(token)
                                                                .build()))
                                        .build();
                    }
                    unitItems.add(
                            OctoDTO.UnitItem.builder()
                                    .uuid(
                                            UUID.nameUUIDFromBytes(
                                                            (octoUuid
                                                                            + "-"
                                                                            + item.getId()
                                                                            + "-"
                                                                            + i)
                                                                    .getBytes())
                                                    .toString())
                                    .unitId(unitId)
                                    .status(mapUnitStatus(octoStatus))
                                    .contact(attendeeDtoToContact(attendee))
                                    .ticket(ticket)
                                    .build());
                }
            }
            if (contact == null && !attendees.isEmpty()) {
                contact = attendeeDtoToContact(attendees.get(0));
            }
        }

        Events event =
                eventHint != null ? eventHint : (primary != null ? primary.getEvent() : null);
        Integer duration = event != null ? event.getDuration() : 60;
        String optionId = appProperties.getOcto().getDefaultOptionId();
        String productId =
                event != null
                        ? event.getRefNo()
                        : (primary != null && primary.getEvent() != null
                                ? primary.getEvent().getRefNo()
                                : null);
        String availabilityId = null;
        OctoAvailabilityIdCodec.AvailabilityKey key = null;
        if (primary != null && productId != null) {
            availabilityId =
                    OctoAvailabilityIdCodec.encode(
                            productId, optionId, primary.getEventDate(), primary.getEventTime());
            key = OctoAvailabilityIdCodec.decode(availabilityId);
        }

        OctoDTO.Availability availability = null;
        if (key != null) {
            ZoneId zone = ZoneId.of(appProperties.getOcto().getTimeZone());
            ZonedDateTime cutoffAt =
                    resolveUtcCutoffAt(event, key.date(), key.time(), zone);
            availability =
                    OctoDTO.Availability.builder()
                            .id(availabilityId)
                            .localDateTimeStart(
                                    OctoAvailabilityIdCodec.toOctoLocalDateTime(
                                            key.date(), key.time()))
                            .localDateTimeEnd(
                                    OctoAvailabilityIdCodec.toOctoLocalDateTimeEnd(
                                            key.date(), key.time(), duration))
                            .utcCutoffAt(formatUtc(cutoffAt))
                            .allDay(false)
                            .openingHours(List.of())
                            .build();
        }

        OctoDTO.Cancellation cancellation = null;
        if (cancelled) {
            cancellation =
                    OctoDTO.Cancellation.builder()
                            .refund("NONE")
                            .reason(cancelReason)
                            .utcCancelledAt(formatUtc(booking.getUpdatedAt()))
                            .build();
        }

        return OctoDTO.Booking.builder()
                .id(octoUuid)
                .uuid(octoUuid)
                .testMode(false)
                .resellerReference(booking.getResellerReference())
                .supplierReference(booking.getRefNo())
                .status(octoStatus)
                .utcCreatedAt(formatUtc(booking.getCreatedAt()))
                .utcUpdatedAt(formatUtc(booking.getUpdatedAt()))
                .utcExpiresAt(formatUtc(booking.getHoldExpiresAt()))
                .utcRedeemedAt(null)
                .utcConfirmedAt(confirmed ? formatUtc(booking.getUpdatedAt()) : null)
                .cancellable(onHold || confirmed)
                .cancellation(cancellation)
                .productId(productId)
                .optionId(optionId)
                .availability(availability)
                .contact(contact)
                .deliveryMethods(List.of("TICKET"))
                .unitItems(unitItems)
                .notes(notesHint)
                .build();
    }

    private static ZonedDateTime resolveUtcCutoffAt(
            Events event, LocalDate date, String normalizedTime, ZoneId zone) {
        LocalTime localTime = LocalTime.parse(OctoAvailabilityIdCodec.normalizeTime(normalizedTime));
        ZonedDateTime eventStart = ZonedDateTime.of(date, localTime, zone);
        if (event == null) {
            return eventStart;
        }
        if (ActivityThresholdUtil.isConfigured(event.getMinActivityHourThreshold())) {
            return eventStart.minusHours(event.getMinActivityHourThreshold());
        }
        if (ActivityThresholdUtil.isConfigured(event.getMinActivityDayThreshold())) {
            return date.minusDays(event.getMinActivityDayThreshold()).atStartOfDay(zone);
        }
        return eventStart;
    }

    private static String toOctoStatus(Enums.BookingStatus status) {
        if (status == null) {
            return "ON_HOLD";
        }
        return switch (status) {
            case CONFIRMED -> "CONFIRMED";
            case CANCELLED -> "CANCELLED";
            case EXPIRED -> "EXPIRED";
            default -> "ON_HOLD";
        };
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
                .fullName(name.isBlank() ? null : name)
                .firstName(attendee.getFirstName())
                .lastName(attendee.getLastName())
                .emailAddress(attendee.getEmail())
                .phoneNumber(attendee.getPhone())
                .country(attendee.getCountry())
                .build();
    }

    private Bookings requireOctoBooking(String uuid) {
        return bookingsRepository
                .findByOctoUuid(uuid)
                .orElseThrow(
                        () ->
                                OctoException.notFound(
                                        "INVALID_BOOKING_UUID", "Booking not found: " + uuid));
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
