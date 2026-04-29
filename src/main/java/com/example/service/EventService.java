package com.example.service;

import com.example.constant.Enums;
import com.example.exception.InvalidVerificationTokenException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.BookingEventsMapper;
import com.example.mapper.EventMapper;
import com.example.mapper.EventTimeSlotExceptionsMapper;
import com.example.mapper.TicketTypeMapper;
import com.example.model.dto.*;
import com.example.model.entity.*;
import com.example.model.record.EventBookingStats;
import com.example.model.record.EventBookingSummary;
import com.example.model.record.EventDailySlot;
import com.example.model.record.EventTimeSlotException;
import com.example.repository.*;
import com.example.utils.DataUtils;
import com.example.utils.DateUtils;
import com.example.utils.ReferenceNoGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.constant.Enums.BookingEventStatus.*;
import static com.example.constant.Enums.EventStatus.*;

@Service
@RequiredArgsConstructor
public class EventService {
    private final ReferenceNoGenerator referenceNoGenerator;
    private final EventsRepository eventsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingAttendeesRepository bookingAttendeesRepository;
    private final BookingItemsRepository bookingItemsRepository;
    private final TicketTypesRepository ticketTypesRepository;
    private final UsersRepository usersRepository;
    private final TicketPricePeriodsRepository ticketPricePeriodsRepository;
    private final EventTimeSlotExceptionsRepository eventTimeSlotExceptionsRepository;
    private final EventMapper eventMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final BookingEventsMapper bookingEventsMapper;
    private final EventTimeSlotExceptionsMapper eventTimeSlotExceptionsMapper;
    private final AwsService awsService;
    private final DateUtils dateUtils;
    private final DataUtils dataUtils;

    @Transactional
    public CreateEventResponseDTO createEvent(CreateEventRequestDTO request, MultipartFile eventPic) throws SQLException, IOException {
        Events event = eventMapper.toEntity(request);
        event.setStatus(Enums.EventStatus.OPEN);
        event.setRefNo(referenceNoGenerator.generateEventReference());
        event.setAvailableDays(new HashSet<>());
        event.setMatchTicketQuantityWithAttendees(request.getMatchTicketQuantityWithAttendees());

        if (request.getAvailableDays() != null) {
            request.getAvailableDays().forEach(dayDto ->
                    dayDto.getStartTimes().stream()
                            .map(startTime -> EventDayScheduleId.builder()
                                    .day(dayDto.getDay())
                                    .startTime(startTime)
                                    .build())
                            .map(id -> EventDaySchedules.builder()
                                    .id(id)
                                    .event(event)
                                    .build())
                            .forEach(event.getAvailableDays()::add)
            );
        }

        Events savedEvent = eventsRepository.save(event);

        if (eventPic != null && !eventPic.isEmpty()) {
            String eventPicKey = awsService.uploadFile(savedEvent.getRefNo(), eventPic);
            savedEvent.setEventPicKey(eventPicKey);
            eventsRepository.save(savedEvent);
        }

        String eventPicUrl = null;
        if(savedEvent.getEventPicKey() != null) {
            eventPicUrl = awsService.getFileFromS3(savedEvent.getEventPicKey());
        }
        CreateEventResponseDTO createEventResponseDTO = eventMapper.toCreateResponseDto(savedEvent);
        createEventResponseDTO.setStatus(Enums.EventStatus.OPEN);
        createEventResponseDTO.setEventPicUrl(eventPicUrl);
        createEventResponseDTO.setMessage("Create Event successfully");
        createEventResponseDTO.setTimestamp(LocalDateTime.now());
        return createEventResponseDTO;
    }

    @Transactional
    public UpdateEventResponseDTO updateEvent(String eventRefNo, UpdateEventRequestDTO dto, MultipartFile eventPic) throws IOException {
        Events event = eventsRepository.findByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with reference no: " + eventRefNo));

        if (dto.getName() != null) event.setName(dto.getName());
        if (dto.getType() != null) event.setType(dto.getType());
        if (dto.getCategory() != null) event.setCategory(dto.getCategory());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getLocation() != null) event.setLocation(dto.getLocation());
        if (dto.getDuration() != null) event.setDuration(dto.getDuration());
        if (dto.getBadge() != null) event.setBadge(dto.getBadge());
        if (dto.getStartDate() != null) event.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) event.setEndDate(dto.getEndDate());
        if (dto.getEquipment() != null) event.setEquipment(dto.getEquipment());
        if (dto.getAvailabilityToEmployeeRatio() != null) event.setAvailabilityToEmployeeRatio(dto.getAvailabilityToEmployeeRatio());
        if (dto.getMaxCapacity() != null) event.setMaxCapacity(dto.getMaxCapacity());
        if (dto.getPrivateBookings() != null) event.setPrivateBookings(dto.getPrivateBookings());
        if (dto.getAdditionalInfo() != null) event.setAdditionalInfo(dto.getAdditionalInfo());
        if (dto.getIsPublish() != null) event.setIsPublish(dto.getIsPublish());
        if (dto.getMinActivityThresholdTime() != null) event.setMinActivityThresholdTime(dto.getMinActivityThresholdTime());
        if (dto.getIsPublish() != null) event.setIsPublish(dto.getIsPublish());
        if (dto.getMaxActivityThresholdTime() != null) event.setMaxActivityThresholdTime(dto.getMaxActivityThresholdTime());
//        if (dto.getCreatedBy() != null) event.setCreatedBy(dto.getCreatedBy());
//        if (dto.getUpdatedBy() != null) event.setUpdatedBy(dto.getUpdatedBy());
        if (dto.getAvailableDays() != null) {
            event.getAvailableDays().clear();
            dto.getAvailableDays().forEach(day -> event.updateDay(event.getId(), day.getDay(), day.getStartTimes()));
        }

        Events updatedEvent = eventsRepository.save(event);

        if (eventPic != null && !eventPic.isEmpty()) {
            if (updatedEvent.getEventPicKey() != null) {
                awsService.deleteFile(updatedEvent.getEventPicKey());
            }
            String eventPicKey = awsService.uploadFile(updatedEvent.getRefNo(), eventPic);
            if (eventPicKey != null) {
                updatedEvent.setEventPicKey(eventPicKey);
                eventsRepository.save(updatedEvent);
            }
        } else if (eventPic == null) {
            if (updatedEvent.getEventPicKey() != null) {
                awsService.deleteFile(updatedEvent.getEventPicKey());
                updatedEvent.setEventPicKey(null);
                eventsRepository.save(updatedEvent);
            }
        }

        String eventPicUrl = null;
        if(updatedEvent.getEventPicKey() != null) {
            eventPicUrl = awsService.getFileFromS3(updatedEvent.getEventPicKey());
        }

        UpdateEventResponseDTO updateEventResponseDTO = eventMapper.toUpdateResponseDto(updatedEvent);
        updateEventResponseDTO.setEventPicUrl(eventPicUrl);
        updateEventResponseDTO.setMessage("Event updated successfully");
        updateEventResponseDTO.setTimestamp(LocalDateTime.now());
        return updateEventResponseDTO;
    }

    @Transactional
    public UpdateEventStatusResponseDTO updateEventStatus(String eventRefNo, UpdateEventStatusRequestDTO updateEventStatusRequestDTO) {
        Events event = eventsRepository.findByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with reference no: " + eventRefNo));

        LocalDateTime actionAt = LocalDateTime.now();
        UpdateEventStatusResponseDTO updateEventStatusResponseDTO = new UpdateEventStatusResponseDTO();
        if (updateEventStatusRequestDTO.getStatus() == CLOSE) {
            EventTimeSlotExceptions eventTimeSlotExceptions = eventTimeSlotExceptionsMapper.toEntity(updateEventStatusRequestDTO, event);
            eventTimeSlotExceptionsRepository.save(eventTimeSlotExceptions);

            updateEventStatusResponseDTO.setStatus(CLOSE);
            updateEventStatusResponseDTO.setClosedAt(actionAt);
            updateEventStatusResponseDTO.setMessage("Event closed successfully");
        } else if (updateEventStatusRequestDTO.getStatus() == CLOSE_WITH_BOOKINGS) {
            EventTimeSlotExceptions eventTimeSlotExceptions = eventTimeSlotExceptionsMapper.toEntity(updateEventStatusRequestDTO, event);
            eventTimeSlotExceptionsRepository.save(eventTimeSlotExceptions);

            bookingEventsRepository.updateCancelStatusBookingsByEventTimeSlot(event.getId(),
                    updateEventStatusRequestDTO.getEventDate(),
                    updateEventStatusRequestDTO.getEventTime(),
                    CANCELLED.toString(),
                    actionAt);

            updateEventStatusResponseDTO.setStatus(CLOSE);
            updateEventStatusResponseDTO.setClosedAt(actionAt);
            updateEventStatusResponseDTO.setMessage("Event closed and related bookings are cancelled successfully");
        } else if (updateEventStatusRequestDTO.getStatus() == OPEN) {
            eventTimeSlotExceptionsRepository.deleteExceptionTimeByEventIdAndDateAndTime(event.getId(),
                    updateEventStatusRequestDTO.getEventDate(),
                    updateEventStatusRequestDTO.getEventTime());
            updateEventStatusResponseDTO.setStatus(OPEN);
            updateEventStatusResponseDTO.setMessage("Event opened successfully");
        } else if (updateEventStatusRequestDTO.getStatus() == OPEN_WITH_BOOKINGS) {
            eventTimeSlotExceptionsRepository.deleteExceptionTimeByEventIdAndDateAndTime(event.getId(),
                    updateEventStatusRequestDTO.getEventDate(),
                    updateEventStatusRequestDTO.getEventTime());

            bookingEventsRepository.updateCancelStatusBookingsByEventTimeSlot(event.getId(),
                    updateEventStatusRequestDTO.getEventDate(),
                    updateEventStatusRequestDTO.getEventTime(),
                    AVAILABLE.toString(),
                    null);

            updateEventStatusResponseDTO.setStatus(OPEN);
            updateEventStatusResponseDTO.setMessage("Event opened and related bookings are restored successfully");
        } else {
            throw new IllegalArgumentException("Invalid EventStatus: " + updateEventStatusRequestDTO.getStatus() +
                    ". Allowed values are: OPEN, CLOSE, OPEN_WITH_BOOKINGS, CLOSE_WITH_BOOKINGS");
        }

        updateEventStatusResponseDTO.setEventRefNo(eventRefNo);
        updateEventStatusResponseDTO.setTimestamp(actionAt);
        return updateEventStatusResponseDTO;
    }

//    @Transactional
//    public UpdateEventStatusResponseDTO updateEventStatus(String eventRefNo, UpdateEventStatusRequestDTO updateEventStatusRequestDTO) {
//        Events event = eventsRepository.findByRefNo(eventRefNo)
//                .orElseThrow(() -> new RuntimeException("Event not found with reference no: " + eventRefNo));
//
//        UpdateEventStatusResponseDTO updateEventStatusResponseDTO = new UpdateEventStatusResponseDTO();
//        if (updateEventStatusRequestDTO.getStatus() == CLOSE) {
//            LocalDateTime deletedAt = LocalDateTime.now();
//            eventsRepository.updateCloseStatusById(eventRefNo, CLOSE, deletedAt);
//            updateEventStatusResponseDTO.setStatus(CLOSE);
//            updateEventStatusResponseDTO.setDeletedAt(deletedAt);
//            updateEventStatusResponseDTO.setMessage("Event deleted successfully");
//            updateEventStatusResponseDTO.setTimestamp(deletedAt);
//        } else {
//            LocalDateTime openedAt = LocalDateTime.now();
//            eventsRepository.updateOpenStatusById(eventRefNo, OPEN, openedAt);
//            updateEventStatusResponseDTO.setMessage("Event opened successfully");
//            updateEventStatusResponseDTO.setTimestamp(openedAt);
//        }
//
//        updateEventStatusResponseDTO.setEventRefNo(eventRefNo);
//        return updateEventStatusResponseDTO;
//    }

    public CreateEventResponseDTO getEvent(String eventRefNo) {
        Events event = eventsRepository.findByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventRefNo));
        String eventPicUrl = null;
        if(event.getEventPicKey() != null) {
            eventPicUrl = awsService.getFileFromS3(event.getEventPicKey());
        }

        CreateEventResponseDTO createEventResponseDTO = eventMapper.toCreateResponseDto(event);
        createEventResponseDTO.setStatus(event.getStatus());
        createEventResponseDTO.setEventPicUrl(eventPicUrl);
        createEventResponseDTO.setMessage("Retrieve an Event successfully");
        createEventResponseDTO.setTimestamp(LocalDateTime.now());
        return createEventResponseDTO;
    }

    public GetListEventResponseDTO getAllEvents(Pageable pageable, String search) {
        Page<Events> eventsPage;

        if (StringUtils.isNotBlank(search)) {
            eventsPage = eventsRepository.findBySearchTerm(search.trim(), pageable);
        } else {
            eventsPage = eventsRepository.findAllActive(pageable);
        }

        List<CreateEventResponseDTO> content = eventsPage.getContent().stream()
                .map(event -> {
                    String eventPicUrl = null;
                    if(event.getEventPicKey() != null) {
                        eventPicUrl = awsService.getFileFromS3(event.getEventPicKey());
                    }
                    CreateEventResponseDTO createEventResponseDTO = eventMapper.toCreateResponseDto(event);
                    createEventResponseDTO.setStatus(event.getStatus());
                    createEventResponseDTO.setEventPicUrl(eventPicUrl);
                    return createEventResponseDTO;
                })
                .toList();

        GetListEventResponseDTO getListEventResponseDTO = eventMapper.toGetListResponse(eventsPage, content);
        getListEventResponseDTO.setMessage("Retrieve list of Events successfully.");
        getListEventResponseDTO.setTimestamp(LocalDateTime.now());
        return getListEventResponseDTO;
    }

    public EventAvailabilityDTO getAvailability(String eventRefNo, LocalDate filterDate) {
        String dayValue = dateUtils.getDayValueForDate(filterDate);

        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with reference no: " + eventRefNo));
        Events event = eventsRepository.findByDateAndId(eventId, filterDate);

        if(event == null) return eventMapper.toGetAvailabilityResponse(event, Collections.emptyMap());

        List<EventDailySlot> allSlots = eventsRepository.getEventScheduleSlots(eventId, filterDate, dayValue);

        List<EventBookingStats> bookingData = getBookingPercentageByDateForEvent(eventId, filterDate, dayValue);

        List<EventTimeSlotException> eventTimeSlotExceptionsByDate = eventTimeSlotExceptionsRepository.findExceptionTimeByEventIdAndExceptionDate(eventId, filterDate);

        Map<String, List<CreateEventResponseDTO.OccupancyDTO>> occupancyMap = eventMapper.toListEventOccupancyMap(eventRefNo, filterDate, allSlots, bookingData, eventTimeSlotExceptionsByDate);

        EventAvailabilityDTO eventAvailabilityDTO = eventMapper.toGetAvailabilityResponse(event, occupancyMap);
        eventAvailabilityDTO.setMessage("Retrieve the availability of event successfully");
        eventAvailabilityDTO.setTimestamp(LocalDateTime.now());
        return eventAvailabilityDTO;
    }

    public GetListEventAvailabilityResponseDTO getAllAvailabilities(Pageable pageable, String search, LocalDate filterDate) {
        String dayValue = dateUtils.getDayValueForDate(filterDate);

        Page<Events> eventsPage;
        if (StringUtils.isNotBlank(search)) {
            eventsPage = eventsRepository.findByDateAndSearch(filterDate, search.trim(), pageable);
        } else {
            eventsPage = eventsRepository.findByDate(filterDate, pageable);
        }

        if (eventsPage.isEmpty()) {
            GetListEventAvailabilityResponseDTO getListEventAvailabilityResponseDTO = eventMapper.toGetListAvailabilitiesResponse(eventsPage, Collections.emptyMap());
            getListEventAvailabilityResponseDTO.setMessage("Retrieve empty list of Availability of event.");
            getListEventAvailabilityResponseDTO.setTimestamp(LocalDateTime.now());
        }

        List<EventDailySlot> allSlots = eventsRepository.getAllEventsScheduleSlots(filterDate, dayValue);

        List<EventBookingStats> bookingData = getBookingPercentageByDate(filterDate, dayValue);

        List<EventTimeSlotException> eventTimeSlotExceptionsByDate = eventTimeSlotExceptionsRepository.findExceptionTimeByExceptionDate(filterDate);

        Map<String, List<CreateEventResponseDTO.OccupancyDTO>> occupancyMap = eventMapper.toListEventOccupancyMap(null, filterDate, allSlots, bookingData, eventTimeSlotExceptionsByDate);

        GetListEventAvailabilityResponseDTO getListEventAvailabilityResponseDTO = eventMapper.toGetListAvailabilitiesResponse(eventsPage, occupancyMap);
        getListEventAvailabilityResponseDTO.setMessage("Retrieve list of Availability of event successfully");
        getListEventAvailabilityResponseDTO.setTimestamp(LocalDateTime.now());
        return getListEventAvailabilityResponseDTO;
    }

    @Transactional
    public CreateTicketTypeResponseDTO createTicketType(String eventRefNo, CreateTicketTypeRequestDTO request) throws SQLException {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with reference no: " + eventRefNo));

        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));

        TicketTypes ticketTypes = ticketTypeMapper.toEntity(request);
        ticketTypes.setRefNo(referenceNoGenerator.generateTicketTypeReference());
        ticketTypes.setEvent(event);

        if (ticketTypes.getTicketPricePeriods() != null) {
            ticketTypes.getTicketPricePeriods().clear();
        }

        ticketTypes = ticketTypesRepository.save(ticketTypes);

        if (request.hasPeriods() && request.getPeriods() != null) {
            for (TicketPricePeriodDTO periodDto : request.getPeriods()) {

                TicketPricePeriods period = new TicketPricePeriods();

                period.setPrice(periodDto.getPrice());
                period.setEffectiveFrom(periodDto.getEffectiveFrom());
                period.setEffectiveTo(periodDto.getEffectiveTo());
                period.setReason(periodDto.getReason() != null ? periodDto.getReason() : "");

                period.setEvent(event);
                period.setTicketTypes(ticketTypes);

                ticketTypes.addTicketPricePeriods(period);

                ticketPricePeriodsRepository.save(period);
            }
        }

        event.addTicketType(ticketTypes);

        eventsRepository.save(event);

        CreateTicketTypeResponseDTO createTicketTypeResponseDTO = ticketTypeMapper.toCreateResponseDto(ticketTypes);
        createTicketTypeResponseDTO.setMessage("Create Ticket Type successfully.");
        createTicketTypeResponseDTO.setTimestamp(LocalDateTime.now());
        return createTicketTypeResponseDTO;
    }

    @Transactional
    public UpdateTicketTypeResponseDTO updateTicketType(String eventRefNo, String ticketTypeRefNo, UpdateTicketTypeRequestDTO request) {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with reference no: " + eventRefNo));

        TicketTypes ticketTypes = ticketTypesRepository.findByRefNo(ticketTypeRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found: " + ticketTypeRefNo));
        Events event = eventsRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if(request.hasPeriods()) {
            List<TicketPricePeriods> newPeriods = request.getPeriods().stream()
                    .map(dto -> TicketPricePeriods.builder()
                            .event(event)
                            .ticketTypes(ticketTypes)
                            .price(dto.getPrice())
                            .effectiveFrom(dto.getEffectiveFrom())
                            .effectiveTo(dto.getEffectiveTo())
                            .reason(dto.getReason())
                            .build())
                    .toList();
            ticketTypes.getPeriods().clear();
            ticketTypes.getPeriods().addAll(newPeriods);
        }

        if(request.getName() != null) ticketTypes.setName(request.getName());
        if(request.getDescription() != null) ticketTypes.setDescription(request.getDescription());

        ticketTypesRepository.save(ticketTypes);

        UpdateTicketTypeResponseDTO updateTicketTypeResponseDTO = ticketTypeMapper.toUpdateResponseDto(ticketTypes);
        updateTicketTypeResponseDTO.setMessage("Update Ticket Type successfully");
        updateTicketTypeResponseDTO.setTimestamp(LocalDateTime.now());
        return updateTicketTypeResponseDTO;
    }

    @Transactional
    public UpdateTicketTypeStatusResponseDTO updateTicketTypeStatus(String eventRefNo, String ticketTypeRefNo, UpdateTicketTypeStatusRequestDTO updateTicketTypeStatusRequestDTO) {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with reference no: " + eventRefNo));

        UpdateTicketTypeStatusResponseDTO updateTicketTypeStatusResponseDTO = new UpdateTicketTypeStatusResponseDTO();
        if(updateTicketTypeStatusRequestDTO.getStatus() == Enums.TicketTypeStatus.CLOSE) {
            LocalDateTime deletedAt = LocalDateTime.now();
            ticketTypesRepository.updateDeleteStatusByEventIdAndTicketTypesRefNo(eventId, ticketTypeRefNo, Enums.TicketTypeStatus.CLOSE, deletedAt);

            updateTicketTypeStatusResponseDTO.setStatus(Enums.TicketTypeStatus.CLOSE);
            updateTicketTypeStatusResponseDTO.setDeletedAt(deletedAt);
            updateTicketTypeStatusResponseDTO.setMessage("Ticket Type deleted successfully");
            updateTicketTypeStatusResponseDTO.setTimestamp(deletedAt);
        } else {
            LocalDateTime openedAt = LocalDateTime.now();
            ticketTypesRepository.updateOpenStatusByEventIdAndTicketTypesRefNo(eventId, ticketTypeRefNo, Enums.TicketTypeStatus.OPEN, openedAt);
            updateTicketTypeStatusResponseDTO.setMessage("Ticket Type opened successfully");
            updateTicketTypeStatusResponseDTO.setTimestamp(openedAt);
        }
        updateTicketTypeStatusResponseDTO.setId(eventRefNo);
        updateTicketTypeStatusResponseDTO.setTicketTypeId(ticketTypeRefNo);
        updateTicketTypeStatusResponseDTO.setTimestamp(LocalDateTime.now());
        return updateTicketTypeStatusResponseDTO;
    }

    public List<CreateTicketTypeResponseDTO> getTicketTypesByEventId(String eventRefNo) {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with reference no: " + eventRefNo));
        if (!eventsRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Event not found with id: " + eventId);
        }

        List<TicketTypes> entities = ticketTypesRepository.findByEventId(eventId);

        return entities.stream()
                .map(ticketTypeMapper::toCreateResponseDto)
                .collect(Collectors.toList());
    }

    public InitiateCheckinResponseDTO initiateCheckin(String token) throws InvalidVerificationTokenException {

        if (token == null || token.trim().isEmpty()) {
            throw new InvalidVerificationTokenException("Verification token is required");
        }

        BookingEvents bookingEvent = bookingEventsRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid Verification token"));

        List<BookingItems> items = bookingItemsRepository.findByBookingEventId(bookingEvent.getId());
        List<CreateBookingRequestDTO.TicketTypeDTO> ticketDTOs = items.stream()
                .map(item -> {
                    String ticketTypeRefNo = ticketTypesRepository.findRefNoById(item.getTicketTypeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found"));

                    CreateBookingRequestDTO.TicketTypeDTO dto = new CreateBookingRequestDTO.TicketTypeDTO();
                    dto.setId(ticketTypeRefNo);
                    dto.setQuantity(item.getQuantity());
                    return dto;
                })
                .toList();

        List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs = bookingAttendeesRepository.findAttendeesByBookingEventId(bookingEvent.getId());

        String userRefNo = null;
        if(bookingEvent.getBooking().getUserId() != null) {
            userRefNo = usersRepository.findRefNoById(bookingEvent.getBooking().getUserId()).orElse(null);
        }

        if (bookingEvent.getVerifiedAt() != null) {
            throw new InvalidVerificationTokenException("Ticket has already been checked in");
        }

        LocalDate date = bookingEvent.getEventDate();
        LocalTime time = LocalTime.parse(bookingEvent.getEventTime());

        LocalDateTime eventStartTime = LocalDateTime.of(date, time);

        if (eventStartTime.isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationTokenException("Ticket has expired");
        }

        return bookingEventsMapper.toResponseDto(userRefNo, bookingEvent, ticketDTOs, attendeeDTOs);
    }

    @Transactional
    public ConfirmCheckinResponseDTO confirmCheckin(ConfirmCheckinRequestDTO request) throws InvalidVerificationTokenException {
        BookingEvents bookingEvent = bookingEventsRepository
                .findByBooking_RefNoAndEvent_RefNoAndEventDateAndEventTime(
                        request.getBookingId(),
                        request.getEventId(),
                        request.getEventDate(),
                        request.getEventTime()
                );

        if (bookingEvent.getVerifiedAt() != null) {
            throw new InvalidVerificationTokenException("Ticket has already been checked in");
        }
        if (bookingEvent.getCancelledAt() != null) {
            throw new InvalidVerificationTokenException("Ticket has already been cancelled");
        }

        bookingEvent.setVerifiedAt(LocalDateTime.now());
        bookingEvent.setUpdatedAt(LocalDateTime.now());
        bookingEvent.setStatus(CHECKED_IN);

        bookingEvent = bookingEventsRepository.save(bookingEvent);

        return ConfirmCheckinResponseDTO.builder()
                        .bookingId(bookingEvent.getBooking().getRefNo())
                        .eventId(bookingEvent.getEvent().getRefNo())
                        .eventDate(bookingEvent.getEventDate())
                        .eventTime(bookingEvent.getEventTime())
                        .status(bookingEvent.getStatus())
                        .verifiedAt(bookingEvent.getVerifiedAt())
                        .message("Confirm Check-in successfully")
                        .timestamp(LocalDateTime.now()).build();
    }

    public List<EventBookingStats> getBookingPercentageByDate(LocalDate filterDate, String dayValue) {
        List<EventDailySlot> slots = eventsRepository.getAllEventsScheduleSlots(filterDate, dayValue);

        return slots.stream().map(slot -> {
            EventBookingSummary summary = eventsRepository.getBookingSummary(
                    slot.eventId(), filterDate, slot.eventTime()
            );

            int maxCap = slot.maxCapacity() != null ? slot.maxCapacity().intValue() : 0;

            BigDecimal bookingPct = dataUtils.calculatePercentage(summary.totalBooked().intValue(), maxCap);
            BigDecimal checkInPct  = dataUtils.calculatePercentage(summary.totalCheckedIn().intValue(), maxCap);

            return new EventBookingStats(
                    slot.eventRef(),
                    slot.eventName(),
                    filterDate,
                    slot.scheduleDay(),
                    slot.eventTime(),
                    maxCap,
                    summary.totalBooked().intValue(),
                    summary.totalCheckedIn().intValue(),
                    bookingPct,
                    checkInPct
            );
        }).collect(Collectors.toList());
    }

    public List<EventBookingStats> getBookingPercentageByDateForEvent(
            Long eventId,
            LocalDate filterDate,
            String dayValue) {

        List<EventDailySlot> slots = eventsRepository.getEventScheduleSlots(eventId, filterDate, dayValue);

        return slots.stream().map(slot -> {
            EventBookingSummary summary = eventsRepository.getBookingSummary(
                    slot.eventId(), filterDate, slot.eventTime()
            );

            int maxCap = slot.maxCapacity() != null ? slot.maxCapacity().intValue() : 0;

            BigDecimal bookingPct = dataUtils.calculatePercentage(summary.totalBooked().intValue(), maxCap);
            BigDecimal checkInPct  = dataUtils.calculatePercentage(summary.totalCheckedIn().intValue(), maxCap);

            return new EventBookingStats(
                    slot.eventRef(),
                    slot.eventName(),
                    filterDate,
                    slot.scheduleDay(),
                    slot.eventTime(),
                    maxCap,
                    summary.totalBooked().intValue(),
                    summary.totalCheckedIn().intValue(),
                    bookingPct,
                    checkInPct
            );
        }).collect(Collectors.toList());
    }
}
