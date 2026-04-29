package com.example.mapper;

import com.example.constant.Enums;
import com.example.model.dto.*;
import com.example.model.entity.EventDaySchedules;
import com.example.model.entity.Events;
import com.example.model.entity.TicketTypes;
import com.example.model.record.EventBookingStats;
import com.example.model.record.EventDailySlot;
import com.example.model.record.EventTimeSlotException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Events toEntity(CreateEventRequestDTO dto);

    default CreateEventResponseDTO toCreateResponseDto(Events entity) {
        if (entity == null) {
            return null;
        }

        CreateEventResponseDTO dto = new CreateEventResponseDTO();

        dto.setId(entity.getRefNo());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setCategory(entity.getCategory());
        dto.setDescription(entity.getDescription());
        dto.setLocation(entity.getLocation());
        dto.setDuration(entity.getDuration());
        dto.setBadge(entity.getBadge());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setAvailableDays(mapAvailableDays(entity.getAvailableDays()));
        dto.setTicketTypes(mapTicketTypes(entity.getTicketTypes()));
        dto.setEquipment(entity.getEquipment());
        dto.setAvailabilityToEmployeeRatio(entity.getAvailabilityToEmployeeRatio());
        dto.setMaxCapacity(entity.getMaxCapacity());
        dto.setPrivateBookings(entity.getPrivateBookings());
        dto.setAdditionalInfo(entity.getAdditionalInfo());
        dto.setMatchTicketQuantityWithAttendees(entity.getMatchTicketQuantityWithAttendees());
        dto.setIsPublish(entity.getIsPublish());
        dto.setMinActivityThresholdTime(entity.getMinActivityThresholdTime());
        dto.setMaxActivityThresholdTime(entity.getMaxActivityThresholdTime());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());

        return dto;
    }

    default Set<AvailableDayDTO> mapAvailableDays(Set<EventDaySchedules> availableDays) {
        if (availableDays == null || availableDays.isEmpty()) {
            return null;
        }

        return availableDays.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getId().getDay(),
                        Collectors.mapping(
                                s -> s.getId().getStartTime(),
                                toList()           // Recommended
                        )
                ))
                .entrySet().stream()
                .map(entry -> AvailableDayDTO.builder()
                        .day(entry.getKey())
                        .startTimes(entry.getValue())
                        .build())
                .collect(Collectors.toSet());
    }

    default List<CreateBookingRequestDTO.TicketTypeDTO> mapTicketTypes(List<TicketTypes> ticketTypes) {
        if (ticketTypes == null || ticketTypes.isEmpty()) {
            return null;
        }

        return ticketTypes.stream()
                .map(TicketTypeMapper::toTicketTypeDTO)
                .toList();
    }

    @Mapping(target="id", source="entity.refNo")
    EventAvailabilityDTO toAvailabilityResponseDto(Events entity);

    @Mapping(target = "availableDays",
            expression = """
        java( entity.getAvailableDays() != null ?
            entity.getAvailableDays().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    s -> s.getWeekday().name(),
                    java.util.stream.Collectors.mapping(
                        s -> s.getId().getStartTime(),
                        java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)
                    )
                ))
                .entrySet().stream()
                .map(entry -> AvailableDayDTO.builder()
                    .day(entry.getKey())
                    .startTimes(new java.util.ArrayList<>(entry.getValue()))
                    .build())
                .collect(java.util.stream.Collectors.toSet())
            : null )
        """)
    @Mapping(target="id", source="entity.refNo")
    UpdateEventResponseDTO toUpdateResponseDto(Events entity);

    default GetListEventResponseDTO toGetListResponse(
            Page<Events> page,
            List<CreateEventResponseDTO> content) {

        GetListEventResponseDTO.PageableDetail pageableDetail = new GetListEventResponseDTO.PageableDetail();
        pageableDetail.setOffset(page.getPageable().getOffset());
        pageableDetail.setPageNumber(page.getPageable().getPageNumber());
        pageableDetail.setPageSize(page.getPageable().getPageSize());
        pageableDetail.setPaged(true);
        pageableDetail.setUnpaged(false);

        GetListEventResponseDTO.SortDetail sortDetail = new GetListEventResponseDTO.SortDetail();
        sortDetail.setEmpty(page.getSort().isEmpty());
        sortDetail.setSorted(page.getSort().isSorted());
        sortDetail.setUnsorted(page.getSort().isUnsorted());
        pageableDetail.setSort(sortDetail);

        return GetListEventResponseDTO.builder()
                .content(content)
                .pageable(pageableDetail)
                .last(page.isLast())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .size(page.getSize())
                .number(page.getNumber())
                .first(page.isFirst())
                .numberOfElements(page.getNumberOfElements())
                .empty(page.isEmpty())
                .build();
    }

    default EventAvailabilityDTO toGetAvailabilityResponse(
            Events event,
            Map<String, List<CreateEventResponseDTO.OccupancyDTO>> occupancyMap) {

        EventAvailabilityDTO dto = toAvailabilityResponseDto(event);
        addOccupancyToEvent(dto, occupancyMap);
        return dto;
    }

    default GetListEventAvailabilityResponseDTO toGetListAvailabilitiesResponse(
            Page<Events> eventsPage,
            Map<String, List<CreateEventResponseDTO.OccupancyDTO>> occupancyMap) {

        List<EventAvailabilityDTO> content = eventsPage.getContent().stream()
                .map(event -> {
                    return toGetAvailabilityResponse(event, occupancyMap);
                })
                .toList();

        // Build PageableDetail
        GetListEventAvailabilityResponseDTO.PageableDetail pageableDetail = new GetListEventAvailabilityResponseDTO.PageableDetail();
        pageableDetail.setOffset(eventsPage.getPageable().getOffset());
        pageableDetail.setPageNumber(eventsPage.getPageable().getPageNumber());
        pageableDetail.setPageSize(eventsPage.getPageable().getPageSize());
        pageableDetail.setPaged(true);
        pageableDetail.setUnpaged(false);

        GetListEventAvailabilityResponseDTO.SortDetail sortDetail = new GetListEventAvailabilityResponseDTO.SortDetail();
        sortDetail.setEmpty(eventsPage.getSort().isEmpty());
        sortDetail.setSorted(eventsPage.getSort().isSorted());
        sortDetail.setUnsorted(eventsPage.getSort().isUnsorted());
        pageableDetail.setSort(sortDetail);

        return GetListEventAvailabilityResponseDTO.builder()
                .content(content)
                .pageable(pageableDetail)
                .last(eventsPage.isLast())
                .totalPages(eventsPage.getTotalPages())
                .totalElements(eventsPage.getTotalElements())
                .size(eventsPage.getSize())
                .number(eventsPage.getNumber())
                .first(eventsPage.isFirst())
                .numberOfElements(eventsPage.getNumberOfElements())
                .empty(eventsPage.isEmpty())
                .build();
    }

    private GetListEventResponseDTO.PageableDetail createPageableDetail(Page<?> page) {
        GetListEventResponseDTO.PageableDetail pd = new GetListEventResponseDTO.PageableDetail();
        pd.setPageNumber(page.getNumber());
        pd.setPageSize(page.getSize());
        pd.setOffset(page.getPageable().getOffset());
        pd.setPaged(page.getPageable().isPaged());
        pd.setUnpaged(page.getPageable().isUnpaged());
        pd.setSort(createSortDetail(page.getSort()));
        return pd;
    }

    private GetListEventResponseDTO.SortDetail createSortDetail(Sort sort) {
        GetListEventResponseDTO.SortDetail sd = new GetListEventResponseDTO.SortDetail();
        sd.setEmpty(sort.isEmpty());
        sd.setSorted(sort.isSorted());
        sd.setUnsorted(sort.isUnsorted());
        return sd;
    }

    default EventAvailabilityDTO addOccupancyToEvent(
            EventAvailabilityDTO dto,
            Map<String, List<CreateEventResponseDTO.OccupancyDTO>> occupancyMap) {

        if (dto == null || occupancyMap == null) {
            return dto;
        }

        List<CreateEventResponseDTO.OccupancyDTO> occupancyList =
                occupancyMap.getOrDefault(dto.getId(), List.of());

        dto.setOccupancy(occupancyList);
        return dto;
    }

    default Map<String, List<CreateEventResponseDTO.OccupancyDTO>> toListEventOccupancyMap(String eventId, LocalDate filterDate, List<EventDailySlot> allSlots,
                                                                                           List<EventBookingStats> bookingData, List<EventTimeSlotException> eventTimeSlotExceptionsByDate) {
        Map<String, List<CreateEventResponseDTO.OccupancyDTO>> occupancyMap = new HashMap<>();

        for (EventDailySlot slot : allSlots) {
            String evtId = eventId == null ? slot.eventRef() : eventId;
            CreateEventResponseDTO.OccupancyDTO occ;
            occ = toEventOccupancyMap(evtId, filterDate, slot.eventTime(), bookingData, eventTimeSlotExceptionsByDate);
            occupancyMap.computeIfAbsent(evtId, k -> new ArrayList<>()).add(occ);
        }
        return occupancyMap;
    }


    default CreateEventResponseDTO.OccupancyDTO toEventOccupancyMap(
            String eventId,
            LocalDate filterDate,
            String eventTime,
            List<EventBookingStats> bookingData,
            List<EventTimeSlotException> exceptionsByDate) {

        Optional<EventBookingStats> bookingOpt = bookingData.stream()
                .filter(b -> Objects.equals(eventId, b.eventId()) &&
                        Objects.equals(eventTime, b.eventTime()))
                .findFirst();

        boolean isCancelled = exceptionsByDate.stream()
                .anyMatch(ex -> Objects.equals(eventId, ex.eventId()) &&
                        Objects.equals(eventTime, ex.eventTime()));

        BigDecimal bookingPercentage = bookingOpt
                .map(EventBookingStats::bookingPercentage)
                .orElse(BigDecimal.ZERO);

        int totalBooked = bookingOpt
                .map(EventBookingStats::totalBooked)
                .orElse(0);

        int totalCheckedIn = bookingOpt
                .map(EventBookingStats::totalCheckedIn)
                .orElse(0);

        Enums.OccupancyStatus status = determineOccupancyStatus(isCancelled, bookingPercentage);

        CreateEventResponseDTO.OccupancyDTO dto = new CreateEventResponseDTO.OccupancyDTO();
        dto.setEventDate(filterDate);
        dto.setEventTime(eventTime);
        dto.setBookingPercentage(bookingPercentage);
        dto.setTotalBooked(totalBooked);
        dto.setTotalCheckedIn(totalCheckedIn);
        dto.setStatus(status);

        return dto;
    }

    private Enums.OccupancyStatus determineOccupancyStatus(boolean isCancelled, BigDecimal percentage) {
        if (isCancelled) {
            return Enums.OccupancyStatus.CANCELLED;
        }
        return percentage.compareTo(BigDecimal.valueOf(100)) >= 0
                ? Enums.OccupancyStatus.FULL
                : Enums.OccupancyStatus.AVAILABLE;
    }
}
