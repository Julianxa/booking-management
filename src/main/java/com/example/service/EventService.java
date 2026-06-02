package com.example.service;

import com.example.constant.Enums;
import com.example.converter.BookingItemsConverter;
import com.example.exception.ticket.InvalidVerificationTokenException;
import com.example.exception.booking.BookingEventNotFoundException;
import com.example.exception.event.EventNotFoundException;
import com.example.exception.general.InvalidJsonFormatException;
import com.example.exception.ticket.TicketTypeNotFoundException;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalTime;
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
    private final EventTimeSlotExceptionsHistoryRepository eventTimeSlotExceptionsHistoryRepository;
    private final UsersRepository usersRepository;
    private final TicketPricePeriodsRepository ticketPricePeriodsRepository;
    private final EventTimeSlotExceptionsRepository eventTimeSlotExceptionsRepository;
    private final EventMapper eventMapper;
    private final TicketTypeMapper ticketTypeMapper;
    private final BookingEventsMapper bookingEventsMapper;
    private final EventTimeSlotExceptionsMapper eventTimeSlotExceptionsMapper;
    private final AwsService awsService;
    private final AuditService auditService;
    private final DateUtils dateUtils;
    private final DataUtils dataUtils;
    private final BookingItemsConverter bookingItemsConverter;

    // ====================== Public API ======================
    @Transactional
    public CreateEventResponseDTO createEvent(String createEventRequestDTOJson, MultipartFile eventPic) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            CreateEventRequestDTO request = mapper.readValue(createEventRequestDTOJson, CreateEventRequestDTO.class);


            Events event = eventMapper.toEntity(request);
            event.setStatus(Enums.EventStatus.OPEN);
            event.setRefNo(referenceNoGenerator.generateEventReference());
            event.setAvailableDays(new HashSet<>());
            event.setMatchTicketQuantityWithAttendees(request.getMatchTicketQuantityWithAttendees());

            addAvailableDaysToEvent(event, request.getAvailableDays());

            Events savedEvent = eventsRepository.save(event);

            String eventPicUrl = null;
            if (eventPic != null && !eventPic.isEmpty()) {
                eventPicUrl = uploadEventPicture(savedEvent, eventPic);
            }

            CreateEventResponseDTO createEventResponseDTO = eventMapper.toCreateResponseDTO(savedEvent);
            createEventResponseDTO.setStatus(Enums.EventStatus.OPEN);
            createEventResponseDTO.setEventPicUrl(eventPicUrl);
            createEventResponseDTO.setMessage("Create Event successfully");
            createEventResponseDTO.setTimestamp(ZonedDateTime.now());

            auditService.record("CREATE_EVENT",
                    Events.class.getName(),
                    savedEvent.getId(),
                    null,
                    savedEvent.getRefNo()
            );
            return createEventResponseDTO;
        } catch (IOException e) {
            throw new InvalidJsonFormatException("Failed to parse event data");
        }
    }

    @Transactional
    public UpdateEventResponseDTO updateEvent(String eventRefNo, String updateEventRequestDTOJson, MultipartFile eventPic) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            UpdateEventRequestDTO dto = mapper.readValue(updateEventRequestDTOJson, UpdateEventRequestDTO.class);

            Events event = eventsRepository.findByRefNo(eventRefNo)
                    .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));

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
            if (dto.getAvailabilityToEmployeeRatio() != null)
                event.setAvailabilityToEmployeeRatio(dto.getAvailabilityToEmployeeRatio());
            if (dto.getMaxCapacity() != null) event.setMaxCapacity(dto.getMaxCapacity());
            if (dto.getPrivateBookings() != null) event.setPrivateBookings(dto.getPrivateBookings());
            if (dto.getAdditionalInfo() != null) event.setAdditionalInfo(dto.getAdditionalInfo());
            if (dto.getCancellationPolicy() != null) event.setCancellationPolicy(dto.getCancellationPolicy());
            if (dto.getCustomQuestion() != null) event.setCustomQuestion(dto.getCustomQuestion());
            if (dto.getMatchTicketQuantityWithAttendees() != null) event.setMatchTicketQuantityWithAttendees(dto.getMatchTicketQuantityWithAttendees());
            if (dto.getIsPublish() != null) event.setIsPublish(dto.getIsPublish());
            if (dto.getActivityDayThreshold() != null)
                event.setActivityDayThreshold(dto.getActivityDayThreshold());
            else if (dto.getActivityHourThreshold() != null)
                event.setActivityHourThreshold(dto.getActivityHourThreshold());
            if (dto.getIsPublish() != null) event.setIsPublish(dto.getIsPublish());
    //        if (dto.getCreatedBy() != null) event.setCreatedBy(dto.getCreatedBy());
    //        if (dto.getUpdatedBy() != null) event.setUpdatedBy(dto.getUpdatedBy());
            if (dto.getAvailableDays() != null) {
                event.getAvailableDays().clear();
                dto.getAvailableDays().forEach(day -> event.updateDay(event.getId(), day.getDay(), day.getStartTimes()));
            }

            handleEventPictureUpdate(event, eventPic);

            Events updatedEvent = eventsRepository.save(event);

            String eventPicUrl = null;
            if (updatedEvent.getEventPicKey() != null) {
                eventPicUrl = awsService.getFileFromS3(updatedEvent.getEventPicKey());
            }

            UpdateEventResponseDTO updateEventResponseDTO = eventMapper.toUpdateResponseDTO(updatedEvent);
            updateEventResponseDTO.setEventPicUrl(eventPicUrl);
            updateEventResponseDTO.setMessage("Event updated successfully");
            updateEventResponseDTO.setTimestamp(ZonedDateTime.now());

            auditService.record("UPDATE_EVENT",
                    Events.class.getName(),
                    updatedEvent.getId(),
                    null,
                    updatedEvent.getRefNo()
            );
            return updateEventResponseDTO;
        } catch(IOException e) {
            throw new InvalidJsonFormatException("Failed to parse event data");
        }

    }

    @Transactional
    public UpdateEventStatusResponseDTO updateEventStatusByDateAndTime(String eventRefNo, UpdateEventStatusRequestDTO updateEventStatusRequestDTO) {
        Events event = eventsRepository.findByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));

        ZonedDateTime actionAt = ZonedDateTime.now();
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
                    AVAILABLE.toString(),
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
                    CANCELLED.toString(),
                    AVAILABLE.toString(),
                    null);

            updateEventStatusResponseDTO.setStatus(OPEN);
            updateEventStatusResponseDTO.setMessage("Event opened and related bookings are restored successfully");
        } else {
            throw new IllegalArgumentException("Invalid EventStatus: " + updateEventStatusRequestDTO.getStatus() +
                    ". Allowed values are: OPEN, CLOSE, OPEN_WITH_BOOKINGS, CLOSE_WITH_BOOKINGS");
        }

        EventTimeSlotExceptionsHistory history = new EventTimeSlotExceptionsHistory();
        history.setEventId(event.getId());
        history.setExceptionDate(updateEventStatusRequestDTO.getEventDate());
        history.setExceptionTime(updateEventStatusRequestDTO.getEventTime());
        history.setStatus(updateEventStatusRequestDTO.getStatus());
        history.setDescription(updateEventStatusRequestDTO.getDescription());
        eventTimeSlotExceptionsHistoryRepository.save(history);

        updateEventStatusResponseDTO.setEventRefNo(eventRefNo);
        updateEventStatusResponseDTO.setTimestamp(actionAt);

        auditService.record("UPDATE_EVENT_STATUS_BY_DATE_AND_TIME",
                Events.class.getName(),
                event.getId(),
                null,
                event.getRefNo()
        );
        return updateEventStatusResponseDTO;
    }

    @Transactional
    public UpdateEventStatusResponseDTO updateEventStatus(String eventRefNo, UpdateEventStatusRequestDTO updateEventStatusRequestDTO) {
        Events event = eventsRepository.findByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));

        ZonedDateTime actionAt = ZonedDateTime.now();
        UpdateEventStatusResponseDTO updateEventStatusResponseDTO = new UpdateEventStatusResponseDTO();
        if (updateEventStatusRequestDTO.getStatus() == CLOSE) {
            event.setStatus(CLOSE);
            event.setDeletedAt(actionAt);
            event.setUpdatedAt(actionAt);
            eventsRepository.save(event);

            updateEventStatusResponseDTO.setStatus(CLOSE);
            updateEventStatusResponseDTO.setClosedAt(actionAt);
            updateEventStatusResponseDTO.setMessage("Event closed successfully");
        } else if (updateEventStatusRequestDTO.getStatus() == CLOSE_WITH_BOOKINGS) {
            event.setStatus(CLOSE);
            event.setDeletedAt(actionAt);
            event.setUpdatedAt(actionAt);
            eventsRepository.save(event);

            bookingEventsRepository.updateCancelStatusBookingsByEventId(event.getId(),
                    AVAILABLE.toString(),
                    CANCELLED.toString(),
                    actionAt);

            updateEventStatusResponseDTO.setStatus(CLOSE);
            updateEventStatusResponseDTO.setClosedAt(actionAt);
            updateEventStatusResponseDTO.setMessage("Event closed and related bookings are cancelled successfully");
        } else if (updateEventStatusRequestDTO.getStatus() == OPEN) {
            event.setStatus(OPEN);
            event.setDeletedAt(null);
            event.setUpdatedAt(actionAt);
            eventsRepository.save(event);

            updateEventStatusResponseDTO.setStatus(OPEN);
            updateEventStatusResponseDTO.setMessage("Event opened successfully");
        } else if (updateEventStatusRequestDTO.getStatus() == OPEN_WITH_BOOKINGS) {
            event.setStatus(OPEN);
            event.setDeletedAt(null);
            event.setUpdatedAt(actionAt);
            eventsRepository.save(event);

            bookingEventsRepository.updateCancelStatusBookingsByEventId(event.getId(),
                    CANCELLED.toString(),
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

        auditService.record("UPDATE_EVENT_STATUS",
                Events.class.getName(),
                event.getId(),
                null,
                event.getRefNo()
        );
        return updateEventStatusResponseDTO;
    }

    public CreateEventResponseDTO getEvent(String eventRefNo) {
        Events event = eventsRepository.findByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));
        String eventPicUrl = null;
        if (event.getEventPicKey() != null) {
            eventPicUrl = awsService.getFileFromS3(event.getEventPicKey());
        }

        CreateEventResponseDTO createEventResponseDTO = eventMapper.toCreateResponseDTO(event);
        createEventResponseDTO.setStatus(event.getStatus());
        createEventResponseDTO.setEventPicUrl(eventPicUrl);
        createEventResponseDTO.setMessage("Retrieve an Event successfully");
        createEventResponseDTO.setTimestamp(ZonedDateTime.now());
        return createEventResponseDTO;
    }

    public GetListEventResponseDTO getAllEvents(boolean isPublishedOnly, Pageable pageable, String search) {
        Page<Events> eventsPage;

        if (StringUtils.isNotBlank(search)) {
            eventsPage = eventsRepository.findBySearchTermWithPublishFilter(isPublishedOnly, search.trim(), pageable);
        } else {
            if (Boolean.TRUE.equals(isPublishedOnly)) {
                eventsPage = eventsRepository.findAllPublished(isPublishedOnly, pageable);
            } else {
                eventsPage = eventsRepository.findAll(pageable);
            }
        }

        List<CreateEventResponseDTO> content = eventsPage.getContent().stream()
                .map(event -> {
                    String eventPicUrl = null;
                    if (event.getEventPicKey() != null) {
                        eventPicUrl = awsService.getFileFromS3(event.getEventPicKey());
                    }
                    CreateEventResponseDTO createEventResponseDTO = eventMapper.toCreateResponseDTO(event);
                    createEventResponseDTO.setStatus(event.getStatus());
                    createEventResponseDTO.setEventPicUrl(eventPicUrl);
                    return createEventResponseDTO;
                })
                .toList();

        GetListEventResponseDTO getListEventResponseDTO = eventMapper.toGetListResponse(eventsPage, content);
        getListEventResponseDTO.setMessage("Retrieve list of Events successfully.");
        getListEventResponseDTO.setTimestamp(ZonedDateTime.now());
        return getListEventResponseDTO;
    }

    public EventAvailabilityDTO getAvailability(boolean isPublishedOnly, String eventRefNo, LocalDate filterDate) {
        String dayValue = dateUtils.getDayValueForDate(filterDate);

        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));
        Events event = eventsRepository.findByDateAndId(eventId, filterDate);

        if (event == null) return eventMapper.toGetAvailabilityResponse(event, Collections.emptyMap());

        List<EventDailySlot> allSlots = eventsRepository.getEventScheduleSlots(isPublishedOnly, eventId, filterDate, dayValue);

        List<EventBookingStats> bookingData = getBookingPercentageByDateForEvent(isPublishedOnly, eventId, filterDate, dayValue);

        List<EventTimeSlotException> eventTimeSlotExceptionsByDate = eventTimeSlotExceptionsRepository.findExceptionTimeByEventIdAndExceptionDate(eventId, filterDate);

        Map<String, List<CreateEventResponseDTO.OccupancyDTO>> occupancyMap = eventMapper.toListEventOccupancyMap(eventRefNo, filterDate, allSlots, bookingData, eventTimeSlotExceptionsByDate);

        EventAvailabilityDTO eventAvailabilityDTO = eventMapper.toGetAvailabilityResponse(event, occupancyMap);
        eventAvailabilityDTO.setMessage("Retrieve the availability of event successfully");
        eventAvailabilityDTO.setTimestamp(ZonedDateTime.now());
        return eventAvailabilityDTO;
    }

    public GetListEventAvailabilityResponseDTO getAllAvailabilities(boolean isPublishedOnly, Pageable pageable, String search, LocalDate filterDate) {
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
            getListEventAvailabilityResponseDTO.setTimestamp(ZonedDateTime.now());
        }

        List<EventDailySlot> allSlots = eventsRepository.getAllEventsScheduleSlots(isPublishedOnly, filterDate, dayValue);

        List<EventBookingStats> bookingData = getBookingPercentageByDate(filterDate, dayValue);

        List<EventTimeSlotException> eventTimeSlotExceptionsByDate = eventTimeSlotExceptionsRepository.findExceptionTimeByExceptionDate(filterDate);

        Map<String, List<CreateEventResponseDTO.OccupancyDTO>> occupancyMap = eventMapper.toListEventOccupancyMap(null, filterDate, allSlots, bookingData, eventTimeSlotExceptionsByDate);

        GetListEventAvailabilityResponseDTO getListEventAvailabilityResponseDTO = eventMapper.toGetListAvailabilitiesResponse(eventsPage, occupancyMap);
        getListEventAvailabilityResponseDTO.setMessage("Retrieve list of Availability of event successfully");
        getListEventAvailabilityResponseDTO.setTimestamp(ZonedDateTime.now());
        return getListEventAvailabilityResponseDTO;
    }

    @Transactional
    public CreateTicketTypeResponseDTO createTicketType(String eventRefNo, CreateTicketTypeRequestDTO request) {
        Events event = eventsRepository.findByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));

        TicketTypes ticketTypes = ticketTypeMapper.toEntity(request);
        ticketTypes.setRefNo(referenceNoGenerator.generateTicketTypeReference());
        ticketTypes.setStatus(Enums.TicketTypeStatus.OPEN);
        ticketTypes.setEvent(event);

        if (ticketTypes.getTicketPricePeriods() != null) {
            ticketTypes.getTicketPricePeriods().clear();
        }

        ticketTypes = ticketTypesRepository.save(ticketTypes);

        if (request.hasPeriods() && request.getPeriods() != null) {
            for (TicketPricePeriodDTO periodDTO : request.getPeriods()) {

                TicketPricePeriods period = new TicketPricePeriods();

                period.setPrice(periodDTO.getPrice());
                period.setEffectiveFrom(periodDTO.getEffectiveFrom());
                period.setEffectiveTo(periodDTO.getEffectiveTo());
                period.setReason(periodDTO.getReason() != null ? periodDTO.getReason() : "");

                period.setEvent(event);
                period.setTicketTypes(ticketTypes);

                ticketTypes.addTicketPricePeriods(period);

                ticketPricePeriodsRepository.save(period);
            }
        }

        event.addTicketType(ticketTypes);

        eventsRepository.save(event);

        CreateTicketTypeResponseDTO createTicketTypeResponseDTO = ticketTypeMapper.toCreateResponseDTO(ticketTypes);
        createTicketTypeResponseDTO.setMessage("Create Ticket Type successfully.");
        createTicketTypeResponseDTO.setTimestamp(ZonedDateTime.now());

        auditService.record("CREATE_TICKET_TYPE",
                TicketTypes.class.getName(),
                ticketTypes.getId(),
                null,
                ticketTypes.getRefNo()
        );
        return createTicketTypeResponseDTO;
    }

    @Transactional
    public UpdateTicketTypeResponseDTO updateTicketType(String eventRefNo, String ticketTypeRefNo, UpdateTicketTypeRequestDTO request) {
        Events event = eventsRepository.findByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));

        TicketTypes ticketTypes = ticketTypesRepository.findByRefNo(ticketTypeRefNo)
                .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", ticketTypeRefNo)));

        if (request.hasPeriods()) {
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

        if (request.getName() != null) ticketTypes.setName(request.getName());
        if (request.getDescription() != null) ticketTypes.setDescription(request.getDescription());

        ticketTypesRepository.save(ticketTypes);

        UpdateTicketTypeResponseDTO updateTicketTypeResponseDTO = ticketTypeMapper.toUpdateResponseDTO(ticketTypes);
        updateTicketTypeResponseDTO.setMessage("Update Ticket Type successfully");
        updateTicketTypeResponseDTO.setTimestamp(ZonedDateTime.now());

        auditService.record("UPDATE_TICKET_TYPE",
                TicketTypes.class.getName(),
                ticketTypes.getId(),
                null,
                ticketTypes.getRefNo()
        );
        return updateTicketTypeResponseDTO;
    }

    @Transactional
    public UpdateTicketTypeStatusResponseDTO updateTicketTypeStatus(String eventRefNo, String ticketTypeRefNo, UpdateTicketTypeStatusRequestDTO updateTicketTypeStatusRequestDTO) {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));

        UpdateTicketTypeStatusResponseDTO updateTicketTypeStatusResponseDTO = new UpdateTicketTypeStatusResponseDTO();
        if (updateTicketTypeStatusRequestDTO.getStatus() == Enums.TicketTypeStatus.CLOSE) {
            ZonedDateTime deletedAt = ZonedDateTime.now();
            ticketTypesRepository.updateDeleteStatusByEventIdAndTicketTypesRefNo(eventId, ticketTypeRefNo, Enums.TicketTypeStatus.CLOSE, deletedAt);

            updateTicketTypeStatusResponseDTO.setStatus(Enums.TicketTypeStatus.CLOSE);
            updateTicketTypeStatusResponseDTO.setDeletedAt(deletedAt);
            updateTicketTypeStatusResponseDTO.setMessage("Ticket Type deleted successfully");
            updateTicketTypeStatusResponseDTO.setTimestamp(deletedAt);
        } else {
            ZonedDateTime openedAt = ZonedDateTime.now();
            ticketTypesRepository.updateOpenStatusByEventIdAndTicketTypesRefNo(eventId, ticketTypeRefNo, Enums.TicketTypeStatus.OPEN, openedAt);
            updateTicketTypeStatusResponseDTO.setMessage("Ticket Type opened successfully");
            updateTicketTypeStatusResponseDTO.setTimestamp(openedAt);
        }
        updateTicketTypeStatusResponseDTO.setId(eventRefNo);
        updateTicketTypeStatusResponseDTO.setTicketTypeId(ticketTypeRefNo);
        updateTicketTypeStatusResponseDTO.setTimestamp(ZonedDateTime.now());
        return updateTicketTypeStatusResponseDTO;
    }

    public List<CreateTicketTypeResponseDTO> getTicketTypesByEventId(String eventRefNo) {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));
        if (!eventsRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Event not found with id: " + eventId);
        }

        List<TicketTypes> ticketTypes = ticketTypesRepository.findByEventId(eventId);

        return ticketTypes.stream()
                .map(ticketTypeMapper::toCreateResponseDTO)
                .collect(Collectors.toList());
    }

    public GetStatusHistoryResponseDTO getEventStatusHistory(String eventRefNo, LocalDate eventDate, String eventTime) {
        Long eventId = eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException("Event not found"));

        List<EventTimeSlotExceptionsHistory> history = eventTimeSlotExceptionsHistoryRepository.findAllByEventIdAndExceptionDateAndExceptionTimeOrderByIdAsc(eventId, eventDate, eventTime)
                .orElse(null);

        GetStatusHistoryResponseDTO getStatusHistoryResponseDTO = new GetStatusHistoryResponseDTO();
        getStatusHistoryResponseDTO.setEventId(eventRefNo);
        getStatusHistoryResponseDTO.setHistory(history);
        getStatusHistoryResponseDTO.setMessage("Retrieve event status history successfully");
        return getStatusHistoryResponseDTO;
    }

    public InitiateCheckInResponseDTO initiateCheckIn(String token) {

        if (token == null || token.trim().isEmpty()) {
            throw new InvalidVerificationTokenException("Verification token is required");
        }

        BookingEvents bookingEvent = bookingEventsRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BookingEventNotFoundException(String.format("Booking Event not found with token %s", token)));
        Events event = eventsRepository.findById(bookingEvent.getEvent().getId())
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", bookingEvent.getEvent().getId())));

        List<BookingItems> bookingItems = bookingItemsRepository.findByBookingEventId(bookingEvent.getId());

        List<CreateBookingRequestDTO.TicketTypeDTO> ticketDTOs = bookingItemsConverter.toTicketTypeDTOs(bookingItems);

        List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs = bookingAttendeesRepository.findAttendeesByBookingEventId(bookingEvent.getId());

        String userRefNo = null;
        if (bookingEvent.getBooking().getUserId() != null) {
            userRefNo = usersRepository.findActiveRefNoById(bookingEvent.getBooking().getUserId()).orElse(null);
        }

        validateCheckIn(bookingEvent);

        CreateBookingRequestDTO.EventDTO eventDTO = eventMapper.toEventDTO(event, bookingEvent);

        return bookingEventsMapper.toInitiateCheckInResponseDTO(userRefNo, eventDTO, bookingEvent, ticketDTOs, attendeeDTOs);
    }

    @Transactional
    public ConfirmCheckinResponseDTO confirmCheckIn(ConfirmCheckinRequestDTO request) {
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

        bookingEvent.setVerifiedAt(ZonedDateTime.now());
        bookingEvent.setUpdatedAt(ZonedDateTime.now());
        bookingEvent.setStatus(CHECKED_IN);

        bookingEvent = bookingEventsRepository.save(bookingEvent);

        auditService.record("CONFIRM_CHECK_IN",
                BookingEvents.class.getName(),
                bookingEvent.getId(),
                null,
                "Check-in confirmed successfully"
        );

        return ConfirmCheckinResponseDTO.builder()
                .bookingId(bookingEvent.getBooking().getRefNo())
                .eventId(bookingEvent.getEvent().getRefNo())
                .eventDate(bookingEvent.getEventDate())
                .eventTime(bookingEvent.getEventTime())
                .status(bookingEvent.getStatus())
                .verifiedAt(bookingEvent.getVerifiedAt())
                .message("Confirm Check-in successfully")
                .timestamp(ZonedDateTime.now()).build();
    }

    // ====================== Private Helper Methods ======================
    private List<EventBookingStats> getBookingPercentageByDate(LocalDate filterDate, String dayValue) {
        List<EventDailySlot> slots = eventsRepository.getAllEventsScheduleSlots(true, filterDate, dayValue);

        return slots.stream().map(slot -> {
            EventBookingSummary summary = eventsRepository.getBookingSummary(
                    slot.eventId(), filterDate, slot.eventTime()
            );

            int maxCap = slot.maxCapacity() != null ? slot.maxCapacity().intValue() : 0;

            BigDecimal bookingPct = dataUtils.calculatePercentage(summary.totalBooked().intValue(), maxCap);
            BigDecimal checkInPct = dataUtils.calculatePercentage(summary.totalCheckedIn().intValue(), maxCap);

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
            boolean isPublishedOnly,
            Long eventId,
            LocalDate filterDate,
            String dayValue) {

        List<EventDailySlot> slots = eventsRepository.getEventScheduleSlots(isPublishedOnly, eventId, filterDate, dayValue);

        return slots.stream().map(slot -> {
            EventBookingSummary summary = eventsRepository.getBookingSummary(
                    slot.eventId(), filterDate, slot.eventTime()
            );

            int maxCap = slot.maxCapacity() != null ? slot.maxCapacity().intValue() : 0;

            BigDecimal bookingPct = dataUtils.calculatePercentage(summary.totalBooked().intValue(), maxCap);
            BigDecimal checkInPct = dataUtils.calculatePercentage(summary.totalCheckedIn().intValue(), maxCap);

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

    private void validateCheckIn(BookingEvents bookingEvent) {
        if (bookingEvent.getVerifiedAt() != null) {
            throw new InvalidVerificationTokenException("Ticket has already been checked in");
        }

        LocalDate date = bookingEvent.getEventDate();
        LocalTime time = LocalTime.parse(bookingEvent.getEventTime());

        ZonedDateTime eventStartTime = ZonedDateTime.of(date, time, ZoneId.systemDefault());

        if (eventStartTime.isBefore(ZonedDateTime.now())) {
            throw new InvalidVerificationTokenException("Ticket has expired");
        }
    }

    private void addAvailableDaysToEvent(Events event, Set<AvailableDayDTO> availableDays) {
        if (availableDays == null || availableDays.isEmpty()) {
            return;
        }

        for (AvailableDayDTO dayDTO : availableDays) {
            if (dayDTO.getStartTimes() == null) continue;

            for (String startTime : dayDTO.getStartTimes()) {
                EventDayScheduleId id = EventDayScheduleId.builder()
                        .day(dayDTO.getDay())
                        .startTime(startTime)
                        .build();

                EventDaySchedules schedule = EventDaySchedules.builder()
                        .id(id)
                        .event(event)
                        .build();

                event.getAvailableDays().add(schedule);
            }
        }
    }

    private String uploadEventPicture(Events savedEvent, MultipartFile eventPic) {
        String eventPicKey = awsService.uploadFile(savedEvent.getRefNo(), eventPic);

        savedEvent.setEventPicKey(eventPicKey);
        eventsRepository.save(savedEvent);

        return awsService.getFileFromS3(eventPicKey);
    }

    private void handleEventPictureUpdate(Events event, MultipartFile eventPic) {
        if (eventPic != null && !eventPic.isEmpty()) {
            if (event.getEventPicKey() != null) {
                awsService.deleteFile(event.getEventPicKey());
            }
            String eventPicKey = awsService.uploadFile(event.getRefNo(), eventPic);
            if (eventPicKey != null) {
                event.setEventPicKey(eventPicKey);
            }
        } else if (eventPic == null) {
            if (event.getEventPicKey() != null) {
                awsService.deleteFile(event.getEventPicKey());
                event.setEventPicKey(null);
            }
        }
    }
}
