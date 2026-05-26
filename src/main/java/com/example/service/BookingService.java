package com.example.service;

import com.example.constant.Enums;
import com.example.converter.BookingItemsConverter;
import com.example.converter.BookingsConverter;
import com.example.exception.booking.BookingNotFoundException;
import com.example.exception.booking.EventTimeSlotUnavailableException;
import com.example.exception.booking.TicketQuantityMismatchException;
import com.example.exception.booking.BookingEventNotFoundException;
import com.example.exception.event.EventCapacityExceededException;
import com.example.exception.event.EventDayScheduleNotFoundException;
import com.example.exception.event.EventNotFoundException;
import com.example.exception.general.MissingRequiredFieldException;
import com.example.exception.giftCertificate.GCRedemptionNotFoundException;
import com.example.exception.ticket.TicketPricePeriodNotFoundException;
import com.example.exception.ticket.TicketTypeNotFoundException;
import com.example.exception.user.UserNotFoundException;
import com.example.mapper.BookingEventsMapper;
import com.example.mapper.BookingMapper;
import com.example.mapper.EventMapper;
import com.example.model.dto.*;
import com.example.model.entity.*;
import com.example.model.record.EventBookingStats;
import com.example.model.record.EventTimeSlotException;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.*;
import com.example.utils.DateUtils;
import com.example.utils.QRCodeGenerator;
import com.example.utils.ReferenceNoGenerator;
import com.example.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.constant.Enums.BookingEventStatus.*;

@Service
@RequiredArgsConstructor
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
    private final EventMapper eventMapper;
    private final EventService eventService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final WebhookService webhookService;
    private final GiftCertificateService giftCertificateService;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final QRCodeGenerator qRCodeGenerator;
    private final DateUtils dateUtils;
    private final UsersRepository usersRepository;
    private final UserUtils userUtils;
    private final GiftCertificateRedemptionRepository giftCertificateRedemptionRepository;
    private final GiftCertificatesRepository giftCertificatesRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BookingItemsConverter bookingItemsConverter;
    private final BookingsConverter bookingsConverter;

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

    // ====================== Public API ======================
    @Transactional
    public CreateBookingResponseDTO createBooking(String userSub, CreateBookingRequestDTO request) {
        validateTicketQuantityMatchesAttendees(request);

        Users loggedInUser = userUtils.getLoggedInUser(userSub);

        Bookings booking = createEmptyBooking(loggedInUser); // PENDING

        BigDecimal grandTotal = processBookingEvents(booking, request.getBookingEvents());

        GiftCertificateApplicationResult gcResult = giftCertificateService.reserveGiftCertificate(loggedInUser, booking, request.getBookingEvents(), request.getPromoCode());

        applyGiftCertificateToBooking(booking, grandTotal, gcResult);

        List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs = bookingsConverter.toBookingEventDTOs(booking, null);

        return handlePostBookingProcessing(loggedInUser, booking, request, bookingEventDTOs);
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

        bookingAttendeesRepository.deleteByBookingEventId(bookingEvent.getId());

        if (request.getAttendees() != null) {
            request.getAttendees().forEach(attendeeDTO ->
                    saveAttendee(bookingEvent.getId(), attendeeDTO)
            );
        }

        if (request.getNotes() != null) {
            bookingEventsRepository.updateNotes(bookingEventId, request.getNotes());
        }

        return UpdateBookingResponseDTO.builder()
                .bookingEventId(bookingEventId)
                .attendees(request.getAttendees())
                .notes(request.getNotes())
                .message("Booking is updated successfully")
                .timestamp(LocalDateTime.now())
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
                .timestamp(LocalDateTime.now()).build();
    }

    public GetListBookingResponseDTO getUserBookings(String userRefNo, Pageable pageable) {
        Long userId = usersRepository.findIdByRefNo(userRefNo)
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found", userRefNo)));

        Page<Bookings> bookingsPage = bookingsRepository.findByUserId(userId, pageable);

        List<CreateBookingResponseDTO> content = bookingsPage.getContent().stream()
                .map(booking -> bookingsConverter.toCreateBookingResponseDTO(booking, null))
                .toList();

        GetListBookingResponseDTO response = bookingMapper.toGetListResponse(bookingsPage, content);
        response.setMessage("Retrieve list of Booking successfully");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public GetListBookingResponseDTO getEventBookings(String eventRefNo, Pageable pageable) {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));

        Page<Bookings> bookingsPage = bookingsRepository.findBookingsByEventId(eventId, pageable);

        List<CreateBookingResponseDTO> content = bookingsPage.getContent().stream()
                .map(booking -> bookingsConverter.toCreateBookingResponseDTO(booking, eventRefNo))
                .toList();

        GetListBookingResponseDTO response = bookingMapper.toGetListResponse(bookingsPage, content);
        response.setMessage("Retrieve list of Booking successfully");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public CreateBookingResponseDTO getBookingById(String bookingRefNo) {
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));

        List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents = bookingsConverter.toBookingEventDTOs(booking, null);

        String giftCertificatePromoCode = null;
        if (booking.getDiscount() != null && booking.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new GCRedemptionNotFoundException("Gift Certificate Redemption not found: " + bookingRefNo));
            giftCertificatePromoCode = giftCertificatesRepository.findPromoCodeById(redemption.getGiftCertificateId());
        }

        return CreateBookingResponseDTO.builder()
                .id(booking.getRefNo())
                .type(booking.getType())
                .totalPaidAmount(booking.getTotalPaidPrice())
                .discount(booking.getDiscount())
                .finalPaidAmount(booking.getFinalPaidAmount())
                .currency(booking.getCurrency())
                .promoCode(giftCertificatePromoCode)
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .bookingEvents(bookingEvents)
                .message("Retrieve booking successfully")
                .timestamp(LocalDateTime.now())
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
        getListParticipantsResponseDTO.setTimestamp(LocalDateTime.now());
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

    private Bookings createEmptyBooking(Users loggedInUser) {
        Bookings booking = Bookings.builder()
                .refNo(referenceNoGenerator.generateBookingReference())
                .userId(loggedInUser != null ? loggedInUser.getId() : null)
                .totalPaidPrice(BigDecimal.ZERO)
                .currency("HKD")
                .status(Enums.BookingStatus.PENDING)
                .build();
        return bookingsRepository.save(booking);
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

        // 5. Capacity Check
        checkEventTimeSlotQuota(dayValue, event, bookingEventDTO);

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
        enrichTicketNames(bookingEventDTO.getTickets());

        CreateBookingRequestDTO.BookingEventDTO responseEventDTO =
                bookingEventsMapper.toCreateResponseDTO(booking, bookingEvent, bookingEventDTO,
                        bookingEventDTO.getAttendees(), null);

        return new BookingEventProcessingResult(responseEventDTO, bookingEventTotal);
    }

    private void enrichTicketNames(List<CreateBookingRequestDTO.TicketTypeDTO> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return;
        }

        for (CreateBookingRequestDTO.TicketTypeDTO ticket : tickets) {
            if (ticket.getName() == null || ticket.getName().isBlank()) {
                TicketTypes ticketType = ticketTypesRepository.findByRefNo(ticket.getId())
                        .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", ticket.getId())));

                ticket.setName(ticketType.getName());
            }
        }
    }

    private void applyGiftCertificateToBooking(Bookings booking,
                                                 BigDecimal grandTotal,
                                                 GiftCertificateApplicationResult gcResult) {

        BigDecimal finalAmount = grandTotal.subtract(gcResult.discount());

        booking.setTotalPaidPrice(grandTotal);
        booking.setFinalPaidAmount(finalAmount);

        if (gcResult.certificate() != null) {
            booking.setGiftCertificateId(gcResult.certificate().getId());
        }
        if (!gcResult.discount().equals(BigDecimal.ZERO)) {
            booking.setDiscount(gcResult.discount());
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

    private void checkEventTimeSlotQuota(String dayValueForDate, Events event, CreateBookingRequestDTO.BookingEventDTO bookingEventDTO) {
        int requestedParticipants = calculateTotalParticipants(bookingEventDTO.getTickets());

        if (requestedParticipants == 0) {
            return;
        }

        List<EventBookingStats> bookingData = eventService.getBookingPercentageByDateForEvent(true, event.getId(), bookingEventDTO.getEvent().getEventDate(), dayValueForDate);
        List<EventTimeSlotException> exceptions = eventTimeSlotExceptionsRepository.findExceptionTimeByEventIdAndExceptionDate(event.getId(), bookingEventDTO.getEvent().getEventDate());
        CreateEventResponseDTO.OccupancyDTO occupancyDTO = eventMapper.toEventOccupancyDTO(
                event.getRefNo(),
                bookingEventDTO.getEvent().getEventDate(),
                bookingEventDTO.getEvent().getEventTime(),
                bookingData,
                exceptions);
        if (occupancyDTO.getTotalBooked() + requestedParticipants > event.getMaxCapacity()) {
            throw new EventCapacityExceededException(String.format("Insufficient capacity for %s on %s at %s. Requested: %d", event.getName(), bookingEventDTO.getEvent().getEventDate(), bookingEventDTO.getEvent().getEventTime(), requestedParticipants));
        }
    }

    public ResendConfirmationEmailResponseDTO reConfirmBooking(String bookinEventId) {

        BookingEvents bookingEvent = bookingEventsRepository.findByRefNo(bookinEventId)
                .orElseThrow(() -> new BookingEventNotFoundException("Booking event not found"));
        Bookings booking = bookingsRepository.findById(bookingEvent.getBooking().getId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
        List<CreateBookingRequestDTO.AttendeeDTO> attendees = bookingAttendeesRepository.findAttendeesByBookingEventId(bookingEvent.getId());
        List<BookingItems> bookingItems = bookingItemsRepository.findByBookingEventId(bookingEvent.getId());

        List<CreateBookingRequestDTO.TicketTypeDTO> ticketDTOs = bookingItemsConverter.toTicketTypeDTOs(bookingItems);

        for (CreateBookingRequestDTO.AttendeeDTO attendeeDTO : attendees) {
            emailService.sendBookingConfirmationEmail(attendeeDTO, booking, bookingEvent, ticketDTOs, attendees);
        }

        return ResendConfirmationEmailResponseDTO.builder()
                .success(true)
                .message("Confirmation email has been resent successfully")
                .bookingEventId(bookinEventId)
                .timestamp(LocalDateTime.now())
                .build();
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

        // book event one by one
        for (CreateBookingRequestDTO.BookingEventDTO bookingEventDTO : bookingEventDTOs) {
            BookingEventProcessingResult result = processSingleBookingEvent(booking, bookingEventDTO);
            grandTotal = grandTotal.add(result.total());
        }
        return grandTotal;
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

    @Transactional
    private CreateBookingResponseDTO handlePostBookingProcessing(Users user, Bookings booking,
                                                                 CreateBookingRequestDTO request,
                                                                 List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {

        if (user == null) { // Public user
            String checkoutUrl = paymentService.createCheckoutSession(null, request, booking);
            booking.setType(Enums.BookingType.ONLINE_PAYMENT);
            bookingsRepository.save(booking);
            return bookingMapper.toCreateResponseDTO(booking, bookingEventDTOs, request.getPromoCode(), checkoutUrl);
        } else { // Admin/Agent flow
            GiftCertificates gc = giftCertificatesRepository.findById(booking.getGiftCertificateId()).orElse(null);
            GiftCertificateApplicationResult result =
                    giftCertificateService.handleGiftCertificateRedemption(booking, gc, user.getId());

            List<EmailService.BookingEmailPayload> emailPayloads = webhookService.activateBookingEvents(bookingEventDTOs);

            booking.setStatus(Enums.BookingStatus.SUCCESS);
            booking.setType(Enums.BookingType.OFFLINE_PAYMENT);
            bookingsRepository.save(booking);

            webhookService.updateBookingStatus(booking, Enums.BookingStatus.SUCCESS);
            webhookService.publishBookingConfirmedEvent(user, booking, bookingEventDTOs, result, emailPayloads);

            return bookingMapper.toCreateResponseDTO(booking, bookingEventDTOs, request.getPromoCode(), null);
        }
    }

    private void updateEventStatusAndPublishEvent(BookingEvents event, Enums.BookingEventStatus newStatus,
                                                  Users user, Bookings booking, List<EmailService.BookingEmailPayload> payloads) {

        event.setStatus(newStatus);
        event.setUpdatedAt(LocalDateTime.now());

        if (newStatus == AVAILABLE) {
            event.setCancelledAt(null);
            bookingEventsRepository.save(event);
            applicationEventPublisher.publishEvent(new EmailService.BookingReConfirmedEvent(user, booking, payloads));
        }
        else if (newStatus == CANCELLED) {
            event.setCancelledAt(LocalDateTime.now());
            bookingEventsRepository.save(event);
            applicationEventPublisher.publishEvent(new BookingCancelledEvent(user, booking, payloads));
        }
        else {
            throw new IllegalArgumentException("Invalid status: " + newStatus +
                    ". Allowed: CHECKED_IN, AVAILABLE, NO_SHOW, CANCELLED");
        }
    }
}