package com.example.service;

import com.example.config.AppProperties;
import com.example.exception.ResourceNotFoundException;
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
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.constant.Enums.BookingEventStatus.*;
import static com.example.constant.Enums.BookingStatus.SUCCESS;
import static com.example.constant.Enums.UserRole.AGENT;

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
    private final EmailService emailService;
    private final GiftCertificateService giftCertificateService;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final AppProperties appProperties;
    private final QRCodeGenerator qRCodeGenerator;
    private final DateUtils dateUtils;
    private final UsersRepository usersRepository;
    private final UserUtils userUtils;
    private final GiftCertificateRedemptionRepository giftCertificateRedemptionRepository;
    private final GiftCertificatesRepository giftCertificatesRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private record BookingEventProcessingResult(
            CreateBookingRequestDTO.BookingEventDTO responseEventDTO,
            BigDecimal total
    ) {}

    private record BookingEmailPayload(
            CreateBookingRequestDTO.AttendeeDTO attendee,
            BookingEvents bookingEvent,
            List<CreateBookingRequestDTO.TicketTypeDTO> tickets,
            List<CreateBookingRequestDTO.AttendeeDTO> allAttendees
    ) {}

    private record BookingCreatedEvent(
            Users loggedInUser,
            Bookings booking,
            List<CreateBookingRequestDTO.BookingEventDTO> responseEvents,
            String promoCode,
            List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets,
            List<BookingEmailPayload> emailPayloads
    ) {}

    @Transactional
    public CreateBookingResponseDTO createBooking(String userSub, CreateBookingRequestDTO request) throws MessagingException, SQLException, BadRequestException {
        validateTicketQuantityMatchesAttendees(request);

        Users loggedInUser = userUtils.getLoggedInUser(userSub);

        Bookings booking = createEmptyBooking(loggedInUser);

        List<CreateBookingRequestDTO.BookingEventDTO> responseEvents = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        List<BookingEmailPayload> emailPayloads = new ArrayList<>();

        for (CreateBookingRequestDTO.BookingEventDTO bookingEventDTO : request.getBookingEvents()) {
            BookingEventProcessingResult result = processSingleBookingEvent(booking, bookingEventDTO);
            responseEvents.add(result.responseEventDTO());
            grandTotal = grandTotal.add(result.total());
            
            if (bookingEventDTO.getAttendees() != null) {
                BookingEvents savedEvent = bookingEventsRepository.findByRefNo(result.responseEventDTO().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking event not found"));
                for (CreateBookingRequestDTO.AttendeeDTO attendee : bookingEventDTO.getAttendees()) {
                    emailPayloads.add(new BookingEmailPayload(attendee, savedEvent, bookingEventDTO.getTickets(), bookingEventDTO.getAttendees()));
                }
            }
        }

        GiftCertificateApplicationResult gcResult = applyGiftCertificateIfPresent(request, loggedInUser, booking);

        updateBookingWithPaymentDetails(booking, grandTotal, gcResult);

        CreateBookingResponseDTO response = bookingMapper.toCreateResponseDto(booking, responseEvents, request.getPromoCode());
        response.setMessage("Create Booking successfully");
        response.setTimestamp(LocalDateTime.now());

        applicationEventPublisher.publishEvent(new BookingCreatedEvent(loggedInUser, booking, responseEvents, request.getPromoCode(), gcResult.redeemedTicketTypes(), emailPayloads));

        return response;
    }

    @Transactional
    public UpdateBookingResponseDTO updateBooking(
            String bookingEventId,
            UpdateBookingRequestDTO request) {
        BookingEvents bookingEvent = bookingEventsRepository.findByRefNo(bookingEventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                "Booking event not found for booking: " + bookingEventId));

        bookingAttendeesRepository.deleteByBookingEventId(bookingEvent.getId());

        if (request.getAttendees() != null) {
            request.getAttendees().forEach(attendeeDto ->
                    saveAttendee(bookingEvent.getId(), attendeeDto)
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
    public UpdateBookingEventStatusResponseDTO updateBookingEventStatus(String bookingEventId, UpdateBookingEventStatusRequestDTO dto) {
        BookingEvents bookingEvents = bookingEventsRepository.findByRefNo(bookingEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Booked event not found"));

        if(bookingEvents.getStatus() == CHECKED_IN) { throw new RuntimeException("Booking is already in CHECKED_IN status."); }

        if(dto.getStatus() != null) {
            if(dto.getStatus() == AVAILABLE) {
                bookingEvents.setStatus(dto.getStatus());
                bookingEvents.setUpdatedAt(LocalDateTime.now());
                bookingEvents.setCancelledAt(null);
            } else if(dto.getStatus() == CANCELLED) {
                bookingEvents.setStatus(dto.getStatus());
                bookingEvents.setUpdatedAt(LocalDateTime.now());
                bookingEvents.setCancelledAt(LocalDateTime.now());
            } else {
                throw new IllegalArgumentException("Invalid BookingEventStatus: " + dto.getStatus() +
                        ". Allowed values are: CHECKED_IN, AVAILABLE, NO_SHOW, CANCELLED");
            }
        }
        bookingEvents = bookingEventsRepository.save(bookingEvents);

        return UpdateBookingEventStatusResponseDTO.builder()
        .bookingId(bookingEvents.getBooking().getRefNo())
        .eventId(bookingEvents.getEvent().getRefNo())
        .eventDate(bookingEvents.getEventDate())
        .eventTime(bookingEvents.getEventTime())
        .status(dto.getStatus())
        .updatedAt(bookingEvents.getUpdatedAt())
        .message("The status of booked event is updated")
        .timestamp(LocalDateTime.now()).build();
    }

    public GetListBookingResponseDTO getUserBookings(String userRefNo, Pageable pageable) {
        Long userId = usersRepository.findIdByRefNo(userRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Bookings> bookingsPage = bookingsRepository.findByUserId(userId, pageable);

        List<CreateBookingResponseDTO> content = bookingsPage.getContent().stream()
                .map(this::mapToCreateBookingResponseDTO)
                .toList();

        GetListBookingResponseDTO response = bookingMapper.toGetListResponse(bookingsPage, content);
        response.setMessage("Retrieve list of Booking successfully");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public GetListBookingResponseDTO getEventBookings(String eventRefNo, Pageable pageable) {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventRefNo));

        Page<Bookings> bookingsPage = bookingsRepository.findBookingsByEventId(eventId, pageable);

        List<CreateBookingResponseDTO> content = bookingsPage.getContent().stream()
                .map(booking -> mapToEventBookingResponseDTO(booking, eventRefNo))
                .toList();

        GetListBookingResponseDTO response = bookingMapper.toGetListResponse(bookingsPage, content);
        response.setMessage("Retrieve list of Booking successfully");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public CreateBookingResponseDTO getBookingById(String bookingRefNo) {
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingRefNo));

        List<BookingEvents> bookingEventsList = bookingEventsRepository.findByBookingId(booking.getId());
        List<CreateBookingRequestDTO.AttendeeDTO> attendees = bookingAttendeesRepository.findAttendeesByBookingId(booking.getId());

        Map<Long, List<BookingItems>> itemsByEvent = buildItemsByEventMap(bookingEventsList);

        List<CreateBookingRequestDTO.BookingEventDTO> events = bookingEventsList.stream()
                .map(be -> buildBookingEventDTO(be, attendees, itemsByEvent, booking, null))
                .toList();

        String giftCertificatePromoCode = null;
        if(booking.getDiscount() != null && booking.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gift Certificate Redemption not found: " + bookingRefNo));
            giftCertificatePromoCode = giftCertificatesRepository.findPromoCodeById(redemption.getGiftCertificateId());
        }

        return CreateBookingResponseDTO.builder()
                .id(booking.getRefNo())
                .totalPaidAmount(booking.getTotalPaidPrice())
                .discount(booking.getDiscount())
                .finalPaidAmount(booking.getFinalPaidAmount())
                .promoCode(giftCertificatePromoCode)
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .bookingEvents(events)
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
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with reference no: " + eventRefNo));

        Page<BookingAttendees> passengers = bookingEventsRepository.findPassengersByEventDateTime(eventId, eventDate, eventTime, pageable);

        GetListParticipantsResponseDTO getListParticipantsResponseDTO = bookingMapper.toGetParticipantsResponse(passengers);
        getListParticipantsResponseDTO.setMessage("Retrieve list of participants successfully");
        getListParticipantsResponseDTO.setTimestamp(LocalDateTime.now());
        return getListParticipantsResponseDTO;
    }

    private void validateTicketQuantityMatchesAttendees(CreateBookingRequestDTO request) throws BadRequestException {
        if (request.getBookingEvents() == null) {
            return;
        }

        for (CreateBookingRequestDTO.BookingEventDTO bookingEvent : request.getBookingEvents()) {
            Events event = eventsRepository.findByRefNo(bookingEvent.getEvent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with reference no: " + bookingEvent.getEvent().getId()));
            if (Boolean.TRUE.equals(event.getMatchTicketQuantityWithAttendees())) {

                int attendeeCount = bookingEvent.getAttendees() != null ? bookingEvent.getAttendees().size() : 0;

                int totalTicketQuantity = calculateTotalTicketQuantity(bookingEvent.getTickets());

                if (attendeeCount != totalTicketQuantity) {
                    throw new BadRequestException(
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

    private Bookings createEmptyBooking(Users loggedInUser) throws SQLException {
        Bookings booking = Bookings.builder()
                .refNo(referenceNoGenerator.generateBookingReference())
                .userId(loggedInUser != null ? loggedInUser.getId() : null)
                .totalPaidPrice(BigDecimal.ZERO)
                .status(SUCCESS)
                .build();
        return bookingsRepository.save(booking);
    }

    private BookingEventProcessingResult processSingleBookingEvent(Bookings booking,
                                                                   CreateBookingRequestDTO.BookingEventDTO bookingEventDTO)
            throws BadRequestException, MessagingException, SQLException {

        Events event = eventsRepository.findByRefNoAndStatusNotDeleted(bookingEventDTO.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active Event not found with reference no: " + bookingEventDTO.getEvent().getId()));

        String dayValue = dateUtils.getDayValueForDate(bookingEventDTO.getEvent().getEventDate());

        eventDaySchedulesRepository.findByEventIdAndDayAndStartTime(
                        event.getId(), dayValue, bookingEventDTO.getEvent().getEventTime())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Schedule not found for event %s on %s at %s",
                                event.getName(), dayValue, bookingEventDTO.getEvent().getEventTime())));

        checkEventTimeSlotQuota(dayValue, event, bookingEventDTO);

        BookingEvents bookingEvent = registerBookingEvent(booking, event, bookingEventDTO);
        BigDecimal bookingEventTotal = registerBookingItemsForBookingEvent(bookingEventDTO, bookingEvent);

        bookingEvent.setTotal(bookingEventTotal);
        bookingEvent = bookingEventsRepository.save(bookingEvent);

        registerAttendeesForEvent(bookingEventDTO, bookingEvent, booking);

        String checkInUrl = appProperties.getBaseUrl() + appProperties.getCheckin().getPath()
                + bookingEvent.getVerificationToken();
        String qrCodeBase64 = qRCodeGenerator.generateQrCodeBase64(checkInUrl);

        CreateBookingRequestDTO.BookingEventDTO responseEventDTO =
                bookingEventsMapper.toCreateResponseDto(booking, bookingEvent, bookingEventDTO,
                        bookingEventDTO.getAttendees(), qrCodeBase64);

        return new BookingEventProcessingResult(responseEventDTO, bookingEventTotal);
    }

    private CreateBookingResponseDTO mapToEventBookingResponseDTO(Bookings booking, String eventRefNo) {
        List<BookingEvents> bookingEventsList = bookingEventsRepository.findByBookingId(booking.getId());

        List<CreateBookingRequestDTO.AttendeeDTO> attendees = bookingAttendeesRepository
                .findAttendeesByBookingId(booking.getId());

        Map<Long, List<BookingItems>> itemsByEvent = buildItemsByEventMap(bookingEventsList);

        List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs = bookingEventsList.stream()
                .map(be -> buildBookingEventDTO(be, attendees, itemsByEvent, booking, eventRefNo))
                .toList();

        String giftCertificatePromoCode = null;
        if(booking.getDiscount() != null && booking.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gift Certificate Redemption not found by booking refNo: " + booking.getRefNo()));
            giftCertificatePromoCode = giftCertificatesRepository.findPromoCodeById(redemption.getGiftCertificateId());
        }

        return CreateBookingResponseDTO.builder()
                .id(booking.getRefNo())
                .totalPaidAmount(booking.getTotalPaidPrice())
                .discount(booking.getDiscount())
                .finalPaidAmount(booking.getFinalPaidAmount())
                .promoCode(giftCertificatePromoCode)
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .bookingEvents(bookingEventDTOs)
                .build();
    }

    private GiftCertificateApplicationResult applyGiftCertificateIfPresent(
            CreateBookingRequestDTO request, Users loggedInUser, Bookings booking) throws BadRequestException {

        if (request.getPromoCode() == null) {
            return new GiftCertificateApplicationResult(null, List.of(), BigDecimal.ZERO);
        }

        Long userId = loggedInUser != null ? loggedInUser.getId() : null;

        GiftCertificates gc = giftCertificateService.validateGiftCertificateForBooking(
                request.getPromoCode(), userId);

        return giftCertificateService.applyGiftCertificateToMultiEventBooking(booking, gc, request, userId);
    }

    private void updateBookingWithPaymentDetails(Bookings booking,
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

    private Map<Long, List<BookingItems>> buildItemsByEventMap(List<BookingEvents> bookingEventsList) {
        Map<Long, List<BookingItems>> map = new HashMap<>();
        for (BookingEvents be : bookingEventsList) {
            map.put(be.getId(), bookingItemsRepository.findByBookingEventId(be.getId()));
        }
        return map;
    }

    private CreateBookingRequestDTO.BookingEventDTO buildBookingEventDTO(
            BookingEvents be,
            List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs,
            Map<Long, List<BookingItems>> itemsByEvent,
            Bookings booking,
            String eventRefNo) {

        List<BookingItems> items = itemsByEvent.getOrDefault(be.getId(), List.of());

        List<CreateBookingRequestDTO.TicketTypeDTO> ticketDTOs = items.stream()
                .map(this::toTicketTypeDTO)
                .toList();

        String checkInUrl = appProperties.getBaseUrl()
                + appProperties.getCheckin().getPath()
                + be.getVerificationToken();
        String qrCodeBase64 = qRCodeGenerator.generateQrCodeBase64(checkInUrl);

        CreateBookingRequestDTO.EventDTO eventDTO;
        if (eventRefNo != null) {
            eventDTO = CreateBookingRequestDTO.EventDTO.builder()
                    .id(eventRefNo)
                    .eventDate(be.getEventDate())
                    .eventTime(be.getEventTime())
                    .build();
        } else {
            eventDTO = bookingEventsMapper.toEventDto(be.getEvent().getRefNo(), be);
        }

        String userRefNo = usersRepository.findRefNoById(booking.getUserId()).orElse(null);

        return CreateBookingRequestDTO.BookingEventDTO.builder()
                .id(be.getRefNo())
                .userId(userRefNo)
                .event(eventDTO)
                .status(be.getStatus())
                .notes(be.getNotes())
                .attendees(attendeeDTOs)
                .tickets(ticketDTOs)
                .qrCodeBase64(qrCodeBase64)
                .build();
    }

    private CreateBookingRequestDTO.TicketTypeDTO toTicketTypeDTO(BookingItems item) {
        TicketTypes ticketType = ticketTypesRepository.findById(item.getTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found"));

        CreateBookingRequestDTO.TicketTypeDTO dto = new CreateBookingRequestDTO.TicketTypeDTO();
        dto.setId(ticketType.getRefNo());
        dto.setQuantity(item.getQuantity());
        dto.setDescription(ticketType.getDescription());
        dto.setStatus(ticketType.getStatus());
        return dto;
    }

    private CreateBookingResponseDTO mapToCreateBookingResponseDTO(Bookings booking) {
        List<BookingEvents> bookingEventsList = bookingEventsRepository.findByBookingId(booking.getId());
        List<CreateBookingRequestDTO.AttendeeDTO> attendees = bookingAttendeesRepository.findAttendeesByBookingId(booking.getId());

        Map<Long, List<BookingItems>> itemsByEvent = buildItemsByEventMap(bookingEventsList);

        List<CreateBookingRequestDTO.BookingEventDTO> events = bookingEventsList.stream()
                .map(be -> buildBookingEventDTO(be, attendees, itemsByEvent, booking, null))
                .toList();

        String giftCertificatePromoCode = null;
        if(booking.getDiscount() != null && booking.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gift Certificate Redemption not found by booking refNo: " + booking.getRefNo()));
            giftCertificatePromoCode = giftCertificatesRepository.findPromoCodeById(redemption.getGiftCertificateId());
        }

        return CreateBookingResponseDTO.builder()
                .id(booking.getRefNo())
                .totalPaidAmount(booking.getTotalPaidPrice())
                .discount(booking.getDiscount())
                .finalPaidAmount(booking.getFinalPaidAmount())
                .promoCode(giftCertificatePromoCode)
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .bookingEvents(events)
                .build();
    }

    public void checkEventTimeSlotQuota(String dayValueForDate, Events event, CreateBookingRequestDTO.BookingEventDTO bookingEventDTO) throws BadRequestException {
        int participantCount = 0;

        for (CreateBookingRequestDTO.TicketTypeDTO ticket : bookingEventDTO.getTickets()) {
            TicketTypes ticketType = ticketTypesRepository.findByRefNo(ticket.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found: " + ticket.getId()));
            ticket.setName(ticketType.getName());
            participantCount += ticket.getQuantity();
        }

        List<EventBookingStats> bookingData = eventService.getBookingPercentageByDateForEvent(event.getId(), bookingEventDTO.getEvent().getEventDate(), dayValueForDate);
        List<EventTimeSlotException> eventTimeSlotExceptionsByDate = eventTimeSlotExceptionsRepository.findExceptionTimeByEventIdAndExceptionDate(event.getId(), bookingEventDTO.getEvent().getEventDate());
        CreateEventResponseDTO.OccupancyDTO occupancyMap = eventMapper.toEventOccupancyMap(event.getRefNo(),
                bookingEventDTO.getEvent().getEventDate(),
                bookingEventDTO.getEvent().getEventTime(),
                bookingData,
                eventTimeSlotExceptionsByDate);
        if(occupancyMap.getTotalBooked() + participantCount > event.getMaxCapacity()) {
            throw new BadRequestException(String.format("Event %s is full on %s at %s", event.getName(), bookingEventDTO.getEvent().getEventDate(), bookingEventDTO.getEvent().getEventTime()));
        }
    }

    public ResendConfirmationEmailResponseDTO reConfirmBooking(String bookinEventId) throws MessagingException {
        BookingEvents bookingEvent = bookingEventsRepository.findByRefNo(bookinEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking event not found"));
        Bookings booking = bookingsRepository.findById(bookingEvent.getBooking().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        List<CreateBookingRequestDTO.AttendeeDTO> attendees = bookingAttendeesRepository.findAttendeesByBookingEventId(bookingEvent.getId());
        List<BookingItems> bookingItems = bookingItemsRepository.findByBookingEventId(bookingEvent.getId());

        List<CreateBookingRequestDTO.TicketTypeDTO> ticketDtos = bookingItems.stream()
                .map(this::toTicketTypeDTO)
                .toList();

        for (CreateBookingRequestDTO.AttendeeDTO attendeeDto : attendees) {
            emailService.sendBookingConfirmationEmail(attendeeDto, booking, bookingEvent, ticketDtos, attendees);
        }

        return ResendConfirmationEmailResponseDTO.builder()
                .success(true)
                .message("Confirmation email has been resent successfully")
                .bookingEventId(bookinEventId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public void registerAttendeesForEvent(CreateBookingRequestDTO.BookingEventDTO bookingEventDTO, BookingEvents bookingEvent, Bookings booking) throws MessagingException {
        for (CreateBookingRequestDTO.AttendeeDTO attendeeDto : bookingEventDTO.getAttendees()) {
            saveAttendee(bookingEvent.getId(), attendeeDto);
        }
    }

    public BigDecimal registerBookingItemsForBookingEvent(CreateBookingRequestDTO.BookingEventDTO bookingEventDTO, BookingEvents bookingEvent) {
        BigDecimal total = BigDecimal.ZERO;
        for (CreateBookingRequestDTO.TicketTypeDTO ticketTypeDTO : bookingEventDTO.getTickets()) {
            Long ticketTypeId = ticketTypesRepository.findIdByRefNo(ticketTypeDTO.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found"));

            TicketPricePeriods periods = ticketPricePeriodsRepository.findActivePrice(ticketTypeId, null)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket Price Period not found"));
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

    public BookingEvents registerBookingEvent(Bookings booking, Events event, CreateBookingRequestDTO.BookingEventDTO bookingEventDTO) throws SQLException {
        bookingEventDTO.getEvent().setName(event.getName());
        BookingEvents bookingEvent = BookingEvents.builder()
                .refNo(referenceNoGenerator.generateBookingEventReference())
                .booking(booking)
                .event(event)
                .eventDate(bookingEventDTO.getEvent().getEventDate())
                .eventTime(bookingEventDTO.getEvent().getEventTime())
                .notes(bookingEventDTO.getNotes())
                .verificationToken(qRCodeGenerator.generateVerificationToken())
                .status(AVAILABLE)
                .build();
        return bookingEventsRepository.save(bookingEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCreatedEvent(BookingCreatedEvent event) {
        try {
            sendBookingOrderSummaryEmailsAsync(event.loggedInUser(), event.booking(), event.responseEvents(), event.promoCode(), event.redeemedTickets(), event.emailPayloads());
            sendBookingConfirmationEmailsAsync(event.booking(), event.emailPayloads());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendBookingOrderSummaryEmailsAsync(Users loggedInUser,
                                                    Bookings booking,
                                                    List<CreateBookingRequestDTO.BookingEventDTO> eventList,
                                                    String promoCode,
                                                    List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets,
                                                    List<BookingEmailPayload> payloads) throws MessagingException {
        if (loggedInUser != null && loggedInUser.getRole() == AGENT) {
            emailService.sendBookingOrderSummaryEmail(loggedInUser, booking, eventList, promoCode, redeemedTickets);
        } else {
            for (BookingEmailPayload payload : payloads) {
                if(payload.attendee().getSequence() == 1) {
                    Users guestAttendee = new Users();
                    guestAttendee.setEmail(payload.attendee().getEmail());
                    guestAttendee.setFirstName(payload.attendee.getFirstName());
                    emailService.sendBookingOrderSummaryEmail(guestAttendee, booking, eventList, promoCode, redeemedTickets);
                }
            }
        }
    }

    private void sendBookingConfirmationEmailsAsync(Bookings booking, List<BookingEmailPayload> payloads) {
        for (BookingEmailPayload payload : payloads) {
            try {
                emailService.sendBookingConfirmationEmail(
                        payload.attendee(),
                        booking,
                        payload.bookingEvent(),
                        payload.tickets(),
                        payload.allAttendees()
                );
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}