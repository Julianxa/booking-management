package com.example.service;

import com.example.constant.Enums;
import com.example.converter.BookingItemsConverter;
import com.example.converter.BookingsConverter;
import com.example.exception.booking.*;
import com.example.exception.email.EmailProcessException;
import com.example.exception.event.EventDayScheduleNotFoundException;
import com.example.exception.event.EventNotFoundException;
import com.example.exception.general.MissingRequiredFieldException;
import com.example.exception.ticket.TicketPricePeriodNotFoundException;
import com.example.exception.ticket.TicketTypeNotFoundException;
import com.example.exception.user.UserNotFoundException;
import com.example.mapper.BookingEventsMapper;
import com.example.mapper.BookingMapper;
import com.example.mapper.TicketTypeMapper;
import com.example.model.dto.*;
import com.example.model.entity.*;
import com.example.model.record.EventTimeSlotException;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.*;
import com.example.utils.ActivityThresholdUtil;
import com.example.utils.DateUtils;
import com.example.utils.PartialUpdateUtil;
import com.example.utils.QRCodeGenerator;
import com.example.utils.ReferenceNoGenerator;
import com.example.utils.UserUtils;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Supplier;

import static com.example.constant.Enums.BookingEmailType.*;
import static com.example.constant.Enums.BookingEventStatus.*;
import static com.example.constant.Enums.BookingStatus.CONFIRMED;
import static com.example.constant.Enums.BookingStatus.PAID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final BookingAttendeesRepository bookingAttendeesRepository;
    private final BookingsRepository bookingsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingItemsRepository bookingItemsRepository;
    private final EventsRepository eventsRepository;
    private final TicketTypesRepository ticketTypesRepository;
    private final TicketPricePeriodsRepository ticketPricePeriodsRepository;
    private final EventDaySchedulesRepository eventDaySchedulesRepository;
    private final EventTimeSlotExceptionsRepository eventTimeSlotExceptionsRepository;
    private final BookingMapper bookingMapper;
    private final BookingEventsMapper bookingEventsMapper;
    private final PaymentService paymentService;
    private final EmailDispatchService emailDispatchService;
    private final EmailService emailService;
    private final WebhookService webhookService;
    private final GiftCertificateService giftCertificateService;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final QRCodeGenerator qRCodeGenerator;
    private final DateUtils dateUtils;
    private final UsersRepository usersRepository;
    private final UserUtils userUtils;
    private final GiftCertificateRedemptionRepository giftCertificateRedemptionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BookingItemsConverter bookingItemsConverter;
    private final BookingsConverter bookingsConverter;
    private final com.example.service.AuditService auditService;
    private final PlatformTransactionManager transactionManager;
    private final EventSlotReservationService eventSlotReservationService;

    public record BookingEventProcessingResult(
            CreateBookingRequestDTO.BookingEventDTO responseEventDTO,
            BigDecimal total
    ) {
    }

    public record BookingCancelledEvent(
            Users loggedInUser,
            Bookings booking,
            List<EmailService.BookingEmailPayload> emailPayloads
    ) {
    }

    private record BookingReservation(
            Users loggedInUser,
            Bookings booking,
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs
    ) {
    }

    private record BookingEventLockOrder(
            CreateBookingRequestDTO.BookingEventDTO bookingEventDTO,
            Long eventId,
            LocalDate eventDate,
            String eventTime
    ) {
    }

    // ====================== Public API ======================
    public CreateBookingResponseDTO createBooking(String userSub, CreateBookingRequestDTO request) {
        validateTicketQuantityMatchesAttendees(request);

        validateEventThreshold(request);

        BookingReservation reservation = reserveBooking(userSub, request);

        try {
            return initiateBookingAndPayment(
                    reservation.loggedInUser(),
                    reservation.booking(),
                    request,
                    reservation.bookingEventDTOs()); // AWAITING_PAYMENT
        } catch (RuntimeException e) {
            failReservedBooking(reservation.booking());
            throw e;
        }
    }

    public Bookings createExternalHold(CreateBookingRequestDTO request) {
        validateTicketQuantityMatchesAttendees(request);
        validateEventThreshold(request);
        BookingReservation reservation = reserveBooking(null, request);
        Bookings booking = reservation.booking();
        booking.setType(Enums.BookingType.OFFLINE_PAYMENT);
        booking.setPlatform(Enums.BookingPlatform.KLOOK);
        return bookingsRepository.save(booking);
    }

    public void confirmExternalHold(Bookings booking) {
        webhookService.confirmOfflinePayment(null, booking);
    }

    public void releaseExternalHold(Bookings booking) {
        failReservedBooking(booking);
    }

    private BookingReservation reserveBooking(String userSub, CreateBookingRequestDTO request) {
        return executeInBookingTransaction(() -> {
            Users loggedInUser = userUtils.getLoggedInUser(userSub);

            Bookings booking = createEmptyBooking(loggedInUser, request); // ON_HOLD

            BigDecimal grandTotal = processBookingEvents(booking, request.getBookingEvents());

            GiftCertificateApplicationResult gcResult = giftCertificateService.validateAndCalculateGiftCertificate(
                    loggedInUser, request.getBookingEvents(), request.getPromoCode(), grandTotal);

            calculateAndUpdateFinalPaymentAmount(booking, grandTotal, gcResult);

            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs = bookingsConverter.toBookingEventDTOs(booking, null);

            if (gcResult.certificate() != null && gcResult.discount().compareTo(BigDecimal.ZERO) > 0) {
                giftCertificateService.preserveGiftCertificate(loggedInUser, booking, gcResult);
            }

            auditService.record("CREATE_BOOKING",
                    Bookings.class.getName(),
                    booking.getId(),
                    loggedInUser != null ? loggedInUser.getId() : null,
                    booking.getRefNo()
            );

            return new BookingReservation(loggedInUser, booking, bookingEventDTOs);
        });
    }

    private void failReservedBooking(Bookings booking) {
        executeInBookingTransaction(() -> {
            Bookings savedBooking = bookingsRepository.findById(booking.getId())
                    .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", booking.getId())));

            eventSlotReservationService.releaseCapacityForBooking(savedBooking);

            giftCertificateService.cancelCertificateRedemption(savedBooking);

            savedBooking.setStatus(Enums.BookingStatus.FAILED);
            bookingsRepository.save(savedBooking);

            return null;
        });
    }

    private <T> T executeInBookingTransaction(Supplier<T> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> action.get());
    }

    @Transactional
    public UpdateBookingResponseDTO updateBooking(
            String userSub,
            String bookingEventId,
            UpdateBookingRequestDTO request) {
        Users loggedInUser = userUtils.getLoggedInUser(userSub);

        BookingEvents bookingEvent = bookingEventsRepository.findByRefNo(bookingEventId)
                .orElseThrow(() -> new BookingEventNotFoundException(
                        String.format("Booking event %s not found", bookingEventId)));

        PartialUpdateUtil.ifPresent(request, "attendees", () -> {
            bookingAttendeesRepository.deleteByBookingEventId(bookingEvent.getId());
            if (request.getAttendees() != null) {
                request.getAttendees().forEach(attendeeDTO -> saveAttendee(bookingEvent.getId(), attendeeDTO));
            }
        });

        PartialUpdateUtil.ifPresent(request, "notes", () ->
                bookingEventsRepository.updateNotes(bookingEventId, request.getNotes()));

        auditService.record("UPDATE_BOOKING",
                BookingEvents.class.getName(),
                bookingEvent.getId(),
                loggedInUser != null ? loggedInUser.getId() : null,
                bookingEvent.getRefNo()
        );

        return UpdateBookingResponseDTO.builder()
                .bookingEventId(bookingEventId)
                .attendees(request.getAttendees())
                .notes(request.hasField("notes") ? request.getNotes() : bookingEvent.getNotes())
                .message("Booking is updated successfully")
                .timestamp(ZonedDateTime.now())
                .build();
    }

    @Transactional
    public UpdateBookingEventStatusResponseDTO updateBookingEventStatus(String userSub, String bookingEventId, UpdateBookingEventStatusRequestDTO dto) {
        Users loggedInUser = userUtils.getLoggedInUser(userSub);

        BookingEvents bookingEvent = bookingEventsRepository.findByRefNo(bookingEventId)
                .orElseThrow(() -> new BookingEventNotFoundException(String.format("Booked event %s not found", bookingEventId)));

        if (bookingEvent.getStatus() == CHECKED_IN) {
            throw new IllegalStateException("Booking is already in CHECKED_IN status.");
        }

        Bookings booking = bookingsRepository.findById(bookingEvent.getBooking().getId())
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingEvent.getBooking().getId())));

        List<EmailService.BookingEmailPayload> emailPayloads = prepareEmailPayloads(bookingEvent);

        updateEventStatusAndPublishEvent(bookingEvent, dto.getStatus(), loggedInUser, booking, emailPayloads);

        return UpdateBookingEventStatusResponseDTO.builder()
                .bookingId(bookingEvent.getBooking().getRefNo())
                .eventId(bookingEvent.getEvent().getRefNo())
                .eventDate(bookingEvent.getEventDate())
                .eventTime(bookingEvent.getEventTime())
                .status(dto.getStatus())
                .updatedAt(bookingEvent.getUpdatedAt())
                .message("The status of booked event is updated")
                .timestamp(ZonedDateTime.now()).build();
    }

    public GetListBookingResponseDTO getUserBookings(String userRefNo, Pageable pageable) {
        Long userId = usersRepository.findIdByRefNo(userRefNo)
                .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", userRefNo)));

        Page<Bookings> bookingsPage = bookingsRepository.findByUserId(userId, pageable);

        List<Bookings> bookings = bookingsPage.getContent();

        List<Long> bookingIds = bookings.stream().map(Bookings::getId).toList();
        Map<Long, String> promoMap = new java.util.HashMap<>();
        if (!bookingIds.isEmpty()) {
            List<Object[]> rows = giftCertificateRedemptionRepository.findPromoCodesByBookingIds(bookingIds);
            for (Object[] r : rows) {
                if (r != null && r.length >= 2 && r[0] != null) {
                    Long bId = ((Number) r[0]).longValue();
                    String promo = r[1] != null ? r[1].toString() : null;
                    promoMap.put(bId, promo);
                }
            }
        }

        List<CreateBookingResponseDTO> content = bookings.stream()
                .map(booking -> bookingsConverter.toCreateBookingResponseDTO(booking, null, promoMap.get(booking.getId())))
                .toList();

        GetListBookingResponseDTO response = bookingMapper.toGetListResponse(bookingsPage, content);
        response.setMessage("Retrieve list of Booking successfully");
        response.setTimestamp(ZonedDateTime.now());
        return response;
    }

    public GetListBookingResponseDTO getEventBookings(String eventRefNo, Pageable pageable) {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));

        Page<Bookings> bookingsPage = bookingsRepository.findBookingsByEventId(eventId, pageable);

        List<Bookings> eventBookings = bookingsPage.getContent();

        List<Long> eventBookingIds = eventBookings.stream().map(Bookings::getId).toList();
        Map<Long, String> eventPromoMap = new java.util.HashMap<>();
        if (!eventBookingIds.isEmpty()) {
            List<Object[]> rows = giftCertificateRedemptionRepository.findPromoCodesByBookingIds(eventBookingIds);
            for (Object[] r : rows) {
                if (r != null && r.length >= 2 && r[0] != null) {
                    Long bId = ((Number) r[0]).longValue();
                    String promo = r[1] != null ? r[1].toString() : null;
                    eventPromoMap.put(bId, promo);
                }
            }
        }

        List<CreateBookingResponseDTO> content = eventBookings.stream()
                .map(booking -> bookingsConverter.toCreateBookingResponseDTO(booking, eventRefNo, eventPromoMap.get(booking.getId())))
                .toList();

        GetListBookingResponseDTO response = bookingMapper.toGetListResponse(bookingsPage, content);
        response.setMessage("Retrieve list of Booking successfully");
        response.setTimestamp(ZonedDateTime.now());
        return response;
    }

    public CreateBookingResponseDTO getBookingById(String bookingRefNo) {
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));

        List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents = bookingsConverter.toBookingEventDTOs(booking, null);

        String giftCertificatePromoCode = null;
        if (booking.getDiscount() != null && booking.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            giftCertificatePromoCode = giftCertificateRedemptionRepository.findPromoCodeByBookingId(booking.getId())
                    .orElse(null);
            
            if (giftCertificatePromoCode == null) {
                log.warn("Booking {} has discount but no gift certificate promo code found", bookingRefNo);
            }
        }

        return CreateBookingResponseDTO.builder()
                .id(booking.getRefNo())
                .type(booking.getType())
                .platform(booking.getPlatform())
                .totalPaidAmount(booking.getTotalPaidPrice())
                .discount(booking.getDiscount())
                .finalPaidAmount(booking.getFinalPaidAmount())
                .currency(booking.getCurrency())
                .language(booking.getLanguage())
                .promoCode(giftCertificatePromoCode)
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .bookingEvents(bookingEvents)
                .message("Retrieve booking successfully")
                .timestamp(ZonedDateTime.now())
                .build();
    }

    public GetListParticipantsResponseDTO getPassengersByEventDateTime(
            String eventRefNo, LocalDate eventDate, String eventTime, Pageable pageable) {
        if (eventRefNo == null || eventDate == null || eventTime == null) {
            throw new IllegalArgumentException("Event ID, date and time are required");
        }
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));

        Page<BookingAttendees> passengers = bookingEventsRepository.findPassengersByEventDateTime(eventId, eventDate, eventTime, pageable);

        GetListParticipantsResponseDTO getListParticipantsResponseDTO = bookingMapper.toGetParticipantsResponse(passengers);
        getListParticipantsResponseDTO.setMessage("Retrieve list of participants successfully");
        getListParticipantsResponseDTO.setTimestamp(ZonedDateTime.now());
        return getListParticipantsResponseDTO;
    }

    // ====================== Private Helper Methods ======================
    private void validateTicketQuantityMatchesAttendees(CreateBookingRequestDTO request) {
        if (request.getBookingEvents() == null) {
            return;
        }

        for (CreateBookingRequestDTO.BookingEventDTO bookingEvent : request.getBookingEvents()) {
            Events event = eventsRepository.findByRefNo(bookingEvent.getEvent().getId())
                    .orElseThrow(() -> new EventNotFoundException(String.format("Event not found with event ID: %s", bookingEvent.getEvent().getId())));

            if (Boolean.TRUE.equals(event.getMatchTicketQuantityWithAttendees())) {
                int attendeeCount = bookingEvent.getAttendees() != null ? bookingEvent.getAttendees().size() : 0;

                int totalTicketQuantity = calculateTotalTicketQuantity(bookingEvent.getTickets());

                if (attendeeCount != totalTicketQuantity) {
                    throw new TicketQuantityMismatchException(
                            String.format("Ticket quantity must match attendee count. " +
                                            "Event: %s | Attendees: %d | Tickets: %d",
                                    bookingEvent.getEvent().getId() != null ? bookingEvent.getEvent().getId() : "Unknown",
                                    attendeeCount,
                                    totalTicketQuantity)
                    );
                }
            }
        }
    }

    /**
     Hour threshold (preferred when set):
     2 hours and event starts at 15:00:
     12:59 → Allowed (121 mins left)
     13:00 → Blocked (120 mins left)

     Day threshold (used only when hour threshold is not set):
     If threshold = 1 and event is on 7 May → Booking only allowed until 5 May
     **/
    private void validateEventThreshold(CreateBookingRequestDTO request) {
        if (request.getBookingEvents() == null) {
            return;
        }

        for (CreateBookingRequestDTO.BookingEventDTO bookingEvent : request.getBookingEvents()) {
            Events event = eventsRepository.findByRefNo(bookingEvent.getEvent().getId())
                    .orElseThrow(() -> new EventNotFoundException(String.format("Event not found with event ID: %s", bookingEvent.getEvent().getId())));

            LocalDate eventDate = bookingEvent.getEvent().getEventDate();
            LocalTime eventTime = LocalTime.parse(bookingEvent.getEvent().getEventTime());
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());

            if (ActivityThresholdUtil.isWithinActivityThreshold(event, eventDate, eventTime, now)) {
                if (ActivityThresholdUtil.isConfigured(event.getActivityHourThreshold())) {
                    throw new ThresholdExceededException(
                            String.format("Booking is not available. " +
                                            "Event: %s | Hour Threshold: %d hour(s). " +
                                            "You must book at least %d hour(s) before the event starts.",
                                    bookingEvent.getEvent().getId() != null ? bookingEvent.getEvent().getId() : "Unknown",
                                    event.getActivityHourThreshold(),
                                    event.getActivityHourThreshold())
                    );
                }
                throw new ThresholdExceededException(
                        String.format("Booking is not allowed. " +
                                        "Event: %s | Day Threshold: %d. " +
                                        "You must book at least %d full day(s) before the event date.",
                                bookingEvent.getEvent().getId() != null ? bookingEvent.getEvent().getId() : "Unknown",
                                event.getActivityDayThreshold(),
                                event.getActivityDayThreshold())
                );
            }
        }
    }

    private int calculateTotalTicketQuantity(List<CreateBookingRequestDTO.TicketTypeDTO> tickets) {
        if (tickets == null) {
            return 0;
        }
        return tickets.stream()
                .filter(t -> t.getQuantity() != null)
                .mapToInt(CreateBookingRequestDTO.TicketTypeDTO::getQuantity)
                .sum();
    }

    private int calculateTotalParticipants(List<CreateBookingRequestDTO.TicketTypeDTO> tickets) {
        if (tickets == null) return 0;
        return tickets.stream()
                .filter(t -> t.getQuantity() != null && t.getQuantity() > 0)
                .mapToInt(CreateBookingRequestDTO.TicketTypeDTO::getQuantity)
                .sum();
    }

    private Bookings createEmptyBooking(Users loggedInUser, CreateBookingRequestDTO request) {
        Bookings booking = Bookings.builder()
                .refNo(referenceNoGenerator.generateBookingReference())
                .type(loggedInUser != null ? Enums.BookingType.OFFLINE_PAYMENT : Enums.BookingType.ONLINE_PAYMENT)
                .platform(Enums.BookingPlatform.WEB)
                .userId(loggedInUser != null ? loggedInUser.getId() : null)
                .totalPaidPrice(BigDecimal.ZERO)
                .currency("HKD")
                .status(Enums.BookingStatus.ON_HOLD)
                .language(request.getLanguage())
                .build();
        booking = bookingsRepository.save(booking);

        auditService.record("ON_HOLD_BOOKING",
                Bookings.class.getName(),
                booking.getId(),
                loggedInUser != null ? loggedInUser.getId() : null,
                booking.getRefNo()
        );
        return booking;
    }

    private BookingEventProcessingResult processSingleBookingEvent(Bookings booking,
                                                                   CreateBookingRequestDTO.BookingEventDTO bookingEventDTO) {

        // 1. Fetch and validate Event
        Events event = eventsRepository.findByRefNoAndOpenStatusAndPublished(bookingEventDTO.getEvent().getId())
                .orElseThrow(() -> new EventNotFoundException(
                        String.format("Active Event %s not found", bookingEventDTO.getEvent().getId())));

        // 2. Validate a specific time slot availability of event
        List<EventTimeSlotException> exceptions = eventTimeSlotExceptionsRepository.findByEventIdAndExceptionDateAndTime(event.getId(), bookingEventDTO.getEvent().getEventDate(), bookingEventDTO.getEvent().getEventTime());
        if(exceptions.size() > 0)
            throw new EventTimeSlotUnavailableException(
                    String.format("The time slot %s at %s is not available for '%s'",
                            bookingEventDTO.getEvent().getEventDate(),
                            bookingEventDTO.getEvent().getEventTime(),
                            event.getName())
            );

        // 3. Validate the answer if question is required
        if(event.getCustomQuestion() != null && bookingEventDTO.getAnswer() == null) {
            throw new MissingRequiredFieldException("Please provide an answer to the question");
        }

        // 4. Validate Schedule
        String dayValue = dateUtils.getDayValueForDate(bookingEventDTO.getEvent().getEventDate());

        eventDaySchedulesRepository.findByEventIdAndDayAndStartTime(
                        event.getId(), dayValue, bookingEventDTO.getEvent().getEventTime())
                .orElseThrow(() -> new EventDayScheduleNotFoundException(
                        String.format("Schedule not found for event %s on %s at %s",
                                event.getName(), dayValue, bookingEventDTO.getEvent().getEventTime())));

        // 5. Atomic capacity reservation (per timeslot counter row)
        reserveEventSlotCapacity(event, bookingEventDTO);

        // 6. Create Booking Event
        BookingEvents bookingEvent = registerBookingEvent(booking, event, bookingEventDTO);

        // 7. Register Items + Calculate Total
        BigDecimal bookingEventTotal = registerBookingItemsForBookingEvent(bookingEventDTO, bookingEvent);

        // 8. Update total
        bookingEvent.setTotal(bookingEventTotal);
        bookingEvent = bookingEventsRepository.save(bookingEvent);

        // 9. Register Attendees
        registerAttendeesForEvent(bookingEventDTO, bookingEvent);

        // 10. Enrich ticket names (this is the right place)
        enrichTicketDetails(bookingEventDTO.getTickets());

        CreateBookingRequestDTO.BookingEventDTO responseEventDTO =
                bookingEventsMapper.toCreateResponseDTO(booking, bookingEvent, bookingEventDTO,
                        bookingEventDTO.getAttendees(), null);

        return new BookingEventProcessingResult(responseEventDTO, bookingEventTotal);
    }

    private void enrichTicketDetails(List<CreateBookingRequestDTO.TicketTypeDTO> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return;
        }

        for (CreateBookingRequestDTO.TicketTypeDTO ticket : tickets) {
            if (ticket.getId() == null || ticket.getId().isBlank()) {
                continue;
            }
            TicketTypes ticketType = ticketTypesRepository.findByRefNo(ticket.getId())
                    .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", ticket.getId())));

            TicketTypeMapper.copyLocalizedFields(ticketType, ticket);
        }
    }

    private void calculateAndUpdateFinalPaymentAmount(Bookings booking,
                                                 BigDecimal grandTotal,
                                                 GiftCertificateApplicationResult gcResult) {

        BigDecimal discount = gcResult.discount() != null
                ? gcResult.discount().min(grandTotal)
                : BigDecimal.ZERO;
        BigDecimal finalAmount = grandTotal.subtract(discount).max(BigDecimal.ZERO);

        booking.setTotalPaidPrice(grandTotal);
        booking.setFinalPaidAmount(finalAmount);

        if (gcResult.certificate() != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            booking.setGiftCertificateId(gcResult.certificate().getId());
            booking.setDiscount(discount);
        }

        bookingsRepository.save(booking);
    }

    private void saveAttendee(Long bookingEventId, CreateBookingRequestDTO.AttendeeDTO dto) {
        BookingAttendees attendee = BookingAttendees.builder()
                .bookingEventId(bookingEventId)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .gender(dto.getGender())
                .country(dto.getCountry())
                .sequence(dto.getSequence())
                .build();
        bookingAttendeesRepository.save(attendee);
    }

    private void reserveEventSlotCapacity(Events event, CreateBookingRequestDTO.BookingEventDTO bookingEventDTO) {
        int requestedParticipants = calculateTotalParticipants(bookingEventDTO.getTickets());
        if (requestedParticipants == 0) {
            return;
        }
        eventSlotReservationService.reserveCapacity(
                event.getId(),
                bookingEventDTO.getEvent().getEventDate(),
                bookingEventDTO.getEvent().getEventTime(),
                requestedParticipants,
                event.getMaxCapacity(),
                event.getName());
    }

    @Transactional(readOnly = true)
    public ResendEventEmailResponseDTO resendEventEmail(String eventId, ResendEventEmailRequestDTO request) {
        LocalDate eventDate = request.getEventDate();
        String eventTime = request.getEventTime();
        String customTemplateRefNo = request.getEmailTemplateId();

        List<BookingEvents> bookingEvents = findEligibleBookingEventsForEventConfirmationResend(eventId, eventDate, eventTime);
        if (bookingEvents.isEmpty()) {
            throw new BookingEventNotFoundException(
                    String.format("No eligible booked events found for %s on %s at %s", eventId, eventDate, eventTime));
        }

        int dispatchedCount = dispatchResendEmailsForBookingEvents(
                bookingEvents, BOOKING_CONFIRMATION, customTemplateRefNo);
        if (dispatchedCount == 0) {
            throw new EmailProcessException(
                    String.format("No attendees found to resend BOOKING_CONFIRMATION email for event %s on %s at %s",
                            eventId, eventDate, eventTime));
        }

        return ResendEventEmailResponseDTO.builder()
                .success(true)
                .message("BOOKING_CONFIRMATION email has been resent successfully")
                .eventId(eventId)
                .eventDate(eventDate)
                .eventTime(eventTime)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public ResendBookingEmailResponseDTO resendBookingEmail(String customTemplateRefNo, String bookingId, Enums.BookingEmailType emailType) {
        if (emailType == null) {
            throw new MissingRequiredFieldException("email_type is required");
        }

        Bookings booking = bookingsRepository.findByRefNo(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (emailType == PAYMENT_CONFIRMATION) {
            if (!isPaidBooking(booking.getStatus())) {
                throw new EmailProcessException(
                        String.format("Booking %s is not in a paid state for %s email resend", bookingId, emailType));
            }
            List<CreateBookingRequestDTO.BookingEventDTO> eventDTOs = bookingsConverter.toBookingEventDTOs(booking, null).stream()
                    .filter(dto -> isResendableBookingEventForConfirmation(dto.getStatus()))
                    .toList();
            if (eventDTOs.isEmpty()) {
                throw new EmailProcessException(
                        String.format("No eligible booking events found to resend %s email for booking %s", emailType, bookingId));
            }
            emailDispatchService.sendPaymentConfirmationEmailsAsync(booking, eventDTOs);
        } else {
            List<BookingEvents> eligibleEvents = findEligibleBookingEventsForResend(booking, emailType);
            if (eligibleEvents.isEmpty()) {
                if (emailType == BOOKING_REMINDER && hasReminderCandidatesWithoutAttainedThreshold(booking)) {
                    throw new ThresholdExceededException(
                            "Activity booking threshold has not been attained yet; reminder email cannot be resent.");
                }
                throw new EmailProcessException(
                        String.format("No eligible booking events found to resend %s email for booking %s", emailType, bookingId));
            }

            int dispatchedCount = dispatchResendEmailsForBookingEvents(eligibleEvents, emailType, customTemplateRefNo);
            if (dispatchedCount == 0) {
                throw new EmailProcessException(
                        String.format("No attendees found to resend %s email for booking %s", emailType, bookingId));
            }
        }

        return ResendBookingEmailResponseDTO.builder()
                .success(true)
                .message(String.format("%s email has been resent successfully", emailType))
                .bookingId(bookingId)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    private List<BookingEvents> findEligibleBookingEventsForEventConfirmationResend(
            String eventRefNo, LocalDate eventDate, String eventTime) {
        return bookingEventsRepository
                .findActiveByEventRefNoAndEventDateAndEventTime(eventRefNo, eventDate, eventTime)
                .orElse(List.of())
                .stream()
                .filter(be -> isEligibleForResend(be.getBooking(), be, BOOKING_CONFIRMATION))
                .toList();
    }

    private List<BookingEvents> findEligibleBookingEventsForResend(Bookings booking, Enums.BookingEmailType emailType) {
        return bookingEventsRepository.findByBookingId(booking.getId()).stream()
                .filter(be -> isEligibleForResend(booking, be, emailType))
                .toList();
    }

    private boolean isEligibleForResend(Bookings booking, BookingEvents bookingEvent, Enums.BookingEmailType emailType) {
        return switch (emailType) {
            case BOOKING_CANCELLATION -> bookingEvent.getStatus() == CANCELLED;
            case BOOKING_REMINDER -> isPaidBooking(booking.getStatus())
                    && bookingEvent.getStatus() == AVAILABLE
                    && bookingEvent.getCancelledAt() == null
                    && isActivityThresholdAttainedForReminder(bookingEvent);
            case BOOKING_CONFIRMATION, PAYMENT_CONFIRMATION -> isPaidBooking(booking.getStatus())
                    && isResendableBookingEventForConfirmation(bookingEvent.getStatus(), bookingEvent.getCancelledAt());
        };
    }

    private boolean isPaidBooking(Enums.BookingStatus bookingStatus) {
        return bookingStatus == PAID || bookingStatus == CONFIRMED;
    }

    private boolean isResendableBookingEventForConfirmation(
            Enums.BookingEventStatus eventStatus,
            ZonedDateTime cancelledAt) {
        return cancelledAt == null
                && (eventStatus == AVAILABLE || eventStatus == CHECKED_IN || eventStatus == NO_SHOW);
    }

    private boolean isResendableBookingEventForConfirmation(Enums.BookingEventStatus eventStatus) {
        return isResendableBookingEventForConfirmation(eventStatus, null);
    }

    private boolean isActivityThresholdAttainedForReminder(BookingEvents bookingEvent) {
        Events event = bookingEvent.getEvent();
        return ActivityThresholdUtil.isActivityThresholdAttainedForReminder(
                event,
                bookingEvent.getEventDate(),
                bookingEvent.getEventTime(),
                ZonedDateTime.now(ZoneId.systemDefault()));
    }

    private boolean hasReminderCandidatesWithoutAttainedThreshold(Bookings booking) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        return bookingEventsRepository.findByBookingId(booking.getId()).stream()
                .anyMatch(be -> isPaidBooking(booking.getStatus())
                        && be.getStatus() == AVAILABLE
                        && be.getCancelledAt() == null
                        && !ActivityThresholdUtil.isActivityThresholdAttainedForReminder(
                                be.getEvent(), be.getEventDate(), be.getEventTime(), now));
    }

    private int dispatchResendEmailsForBookingEvents(
            List<BookingEvents> bookingEvents,
            Enums.BookingEmailType emailType,
            String customTemplateRefNo) {
        int dispatchedCount = 0;

        for (BookingEvents bookingEvent : bookingEvents) {
            List<EmailService.BookingEmailPayload> payloads = prepareEmailPayloads(bookingEvent);
            if (payloads.isEmpty()) {
                continue;
            }

            Bookings booking = bookingEvent.getBooking();
            switch (emailType) {
                case BOOKING_CONFIRMATION -> emailDispatchService.sendCustomOrBookingConfirmationEmailsAsync(
                        booking, payloads, customTemplateRefNo);
                case BOOKING_CANCELLATION -> emailDispatchService.sendBookingCancellationEmailsAsync(booking, payloads);
                case BOOKING_REMINDER -> emailDispatchService.sendBookingReminderEmailsAsync(booking, payloads);
                default -> throw new EmailProcessException("Unsupported email type for booking event resend: " + emailType);
            }
            dispatchedCount++;
        }

        return dispatchedCount;
    }

    private void registerAttendeesForEvent(CreateBookingRequestDTO.BookingEventDTO bookingEventDTO, BookingEvents bookingEvent) {
        for (CreateBookingRequestDTO.AttendeeDTO attendeeDTO : bookingEventDTO.getAttendees()) {
            saveAttendee(bookingEvent.getId(), attendeeDTO);
        }
    }

    private BigDecimal registerBookingItemsForBookingEvent(CreateBookingRequestDTO.BookingEventDTO bookingEventDTO, BookingEvents bookingEvent) {
        BigDecimal total = BigDecimal.ZERO;
        for (CreateBookingRequestDTO.TicketTypeDTO ticketTypeDTO : bookingEventDTO.getTickets()) {
            Long ticketTypeId = ticketTypesRepository.findIdByRefNo(ticketTypeDTO.getId())
                    .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", ticketTypeDTO.getId())));

            TicketPricePeriods periods = ticketPricePeriodsRepository.findActivePrice(ticketTypeId, null)
                    .orElseThrow(() -> new TicketPricePeriodNotFoundException(String.format("Ticket Price not found with Ticket Type %s", ticketTypeDTO.getId())));
            BigDecimal subtotal = periods.getPrice().multiply(BigDecimal.valueOf(ticketTypeDTO.getQuantity()));
            total = total.add(subtotal);

            BookingItems bookingItem = BookingItems.builder()
                    .bookingEventId(bookingEvent.getId())
                    .ticketTypeId(ticketTypeId)
                    .quantity(ticketTypeDTO.getQuantity())
                    .subtotal(subtotal)
                    .build();
            bookingItemsRepository.save(bookingItem);
        }
        return total;
    }

    private BookingEvents registerBookingEvent(Bookings booking, Events event, CreateBookingRequestDTO.BookingEventDTO bookingEventDTO) {
        bookingEventDTO.getEvent().setName(event.getName());
        bookingEventDTO.getEvent().setNameZhHk(event.getNameZhHk());
        bookingEventDTO.getEvent().setNameZhCn(event.getNameZhCn());
        BookingEvents bookingEvent = BookingEvents.builder()
                .refNo(referenceNoGenerator.generateBookingEventReference())
                .booking(booking)
                .event(event)
                .eventDate(bookingEventDTO.getEvent().getEventDate())
                .eventTime(bookingEventDTO.getEvent().getEventTime())
                .notes(bookingEventDTO.getNotes())
                .answer(bookingEventDTO.getAnswer())
                .verificationToken(qRCodeGenerator.generateVerificationToken())
                .status(PENDING)
                .build();
        return bookingEventsRepository.save(bookingEvent);
    }

    private BigDecimal processBookingEvents(Bookings booking, List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {

        BigDecimal grandTotal = BigDecimal.ZERO;

        // Reserve slot counters in deterministic order to avoid deadlocks for multi-event bookings.
        for (CreateBookingRequestDTO.BookingEventDTO bookingEventDTO : orderBookingEventsForSlotReservation(bookingEventDTOs)) {
            BookingEventProcessingResult result = processSingleBookingEvent(booking, bookingEventDTO);
            grandTotal = grandTotal.add(result.total());
        }
        return grandTotal;
    }

    private List<CreateBookingRequestDTO.BookingEventDTO> orderBookingEventsForSlotReservation(
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
        if (bookingEventDTOs == null || bookingEventDTOs.isEmpty()) {
            return List.of();
        }

        return bookingEventDTOs.stream()
                .map(bookingEventDTO -> {
                    String eventRefNo = bookingEventDTO.getEvent().getId();
                    Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                            .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));
                    return new BookingEventLockOrder(
                            bookingEventDTO,
                            eventId,
                            bookingEventDTO.getEvent().getEventDate(),
                            bookingEventDTO.getEvent().getEventTime());
                })
                .sorted(Comparator
                        .comparing(BookingEventLockOrder::eventId)
                        .thenComparing(BookingEventLockOrder::eventDate)
                        .thenComparing(BookingEventLockOrder::eventTime))
                .map(BookingEventLockOrder::bookingEventDTO)
                .toList();
    }

    private List<EmailService.BookingEmailPayload> prepareEmailPayloads(BookingEvents bookingEvent) {
        List<CreateBookingRequestDTO.AttendeeDTO> attendees =
                bookingAttendeesRepository.findAttendeesByBookingEventId(bookingEvent.getId());

        List<BookingItems> items = bookingItemsRepository.findByBookingEventId(bookingEvent.getId());
        List<CreateBookingRequestDTO.TicketTypeDTO> ticketDTOs = bookingItemsConverter.toTicketTypeDTOs(items);

        return attendees.stream()
                .map(att -> new EmailService.BookingEmailPayload(att, bookingEvent, ticketDTOs, attendees))
                .toList();
    }

    private CreateBookingResponseDTO initiateBookingAndPayment(Users user, Bookings booking,
                                                                 CreateBookingRequestDTO request,
                                                                 List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
        if (user == null) { // guest
            String checkoutUrl = null;

            if (booking.getFinalPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                Session session = paymentService.createCheckoutSession(null, request, booking); // AWAITING_PAYMENT
                checkoutUrl = session.getUrl();
                paymentService.findOrCreatePaymentByPaymentIntentId(session.getId(), null, null, booking);
            } else { // 0 dollar payment
                Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(null, null, null, booking);

                ZonedDateTime paidAt = ZonedDateTime.now();
                webhookService.confirmOnlinePayment(null, booking, payment, null, null, paidAt);
            }
            return bookingMapper.toCreateResponseDTO(booking, bookingEventDTOs, request.getPromoCode(), checkoutUrl);
        } else { // Admin/Agent flow
            webhookService.confirmOfflinePayment(user, booking);
            return bookingMapper.toCreateResponseDTO(booking, bookingEventDTOs, request.getPromoCode(), null);
        }
    }

    private void updateEventStatusAndPublishEvent(BookingEvents bookingEvent, Enums.BookingEventStatus newStatus,
                                                  Users user, Bookings booking, List<EmailService.BookingEmailPayload> payloads) {

        Enums.BookingEventStatus previousStatus = bookingEvent.getStatus();
        Events parentEvent = bookingEvent.getEvent();

        bookingEvent.setStatus(newStatus);
        bookingEvent.setUpdatedAt(ZonedDateTime.now());

        if (newStatus == AVAILABLE) {
            if (previousStatus == CANCELLED) {
                eventSlotReservationService.reserveCapacityForBookingEvent(
                        bookingEvent, parentEvent.getMaxCapacity(), parentEvent.getName());
            }
            bookingEvent.setCancelledAt(null);
            bookingEventsRepository.save(bookingEvent);
            applicationEventPublisher.publishEvent(new EmailService.BookingRestoreEvent(user, booking, payloads));
        }
        else if (newStatus == CANCELLED) {
            if (previousStatus != CANCELLED) {
                eventSlotReservationService.releaseCapacityForBookingEvent(bookingEvent);
            }
            bookingEvent.setCancelledAt(ZonedDateTime.now());
            bookingEventsRepository.save(bookingEvent);
            applicationEventPublisher.publishEvent(new BookingCancelledEvent(user, booking, payloads));
        }
        else {
            throw new IllegalArgumentException("Invalid status: " + newStatus +
                    ". Allowed: CHECKED_IN, AVAILABLE, NO_SHOW, CANCELLED");
        }
    }
}