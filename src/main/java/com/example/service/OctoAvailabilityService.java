package com.example.service;

import com.example.config.AppProperties;
import com.example.exception.octo.OctoException;
import com.example.model.dto.OctoDTO;
import com.example.model.entity.EventDaySchedules;
import com.example.model.entity.Events;
import com.example.repository.EventDaySchedulesRepository;
import com.example.repository.EventTimeSlotExceptionsRepository;
import com.example.utils.ActivityThresholdUtil;
import com.example.utils.DateUtils;
import com.example.utils.OctoAvailabilityIdCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OctoAvailabilityService {

    private static final DateTimeFormatter UTC_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final AppProperties appProperties;
    private final OctoCatalogService octoCatalogService;
    private final EventSlotReservationService eventSlotReservationService;
    private final EventDaySchedulesRepository eventDaySchedulesRepository;
    private final EventTimeSlotExceptionsRepository eventTimeSlotExceptionsRepository;
    private final DateUtils dateUtils;

    @Transactional(readOnly = true)
    public List<OctoDTO.Availability> checkAvailability(OctoDTO.AvailabilityRequest request) {
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            throw OctoException.badRequest("INVALID_PRODUCT_ID", "productId is required");
        }
        String optionId =
                request.getOptionId() == null || request.getOptionId().isBlank()
                        ? appProperties.getOcto().getDefaultOptionId()
                        : request.getOptionId();
        octoCatalogService.assertOptionId(optionId);
        Events event = octoCatalogService.requirePublishedEvent(request.getProductId());

        LocalDate start = parseDate(request.getLocalDateStart(), "localDateStart");
        LocalDate end = parseDate(request.getLocalDateEnd(), "localDateEnd");
        if (end.isBefore(start)) {
            throw OctoException.badRequest("INVALID_DATE_RANGE", "localDateEnd before localDateStart");
        }

        ZoneId zone = ZoneId.of(appProperties.getOcto().getTimeZone());
        ZonedDateTime now = ZonedDateTime.now(zone);
        int requestedQty = requestedQuantity(request.getUnits());
        Map<String, List<String>> timesByWeekday = loadTimesByWeekday(event.getId());
        Set<String> cancelledSlots = loadCancelledSlots(event.getId(), start, end);
        Map<String, Integer> reservedBySlot =
                eventSlotReservationService.getReservedQtyBySlotKey(event.getId(), start, end);
        int capacity = event.getMaxCapacity() != null ? event.getMaxCapacity() : 0;

        List<OctoDTO.Availability> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (!isEventActiveOnDate(event, date)) {
                continue;
            }
            String weekday = dateUtils.getDayValueForDate(date);
            for (String time : timesByWeekday.getOrDefault(weekday, List.of())) {
                String normalizedTime = OctoAvailabilityIdCodec.normalizeTime(time);
                String key = slotKey(date, time);
                boolean cancelled = cancelledSlots.contains(key);
                int reserved = reservedBySlot.getOrDefault(key, 0);
                int vacancies = cancelled ? 0 : Math.max(0, capacity - reserved);
                ZonedDateTime cutoffAt = resolveUtcCutoffAt(event, date, normalizedTime, zone);
                boolean pastCutoff = !now.isBefore(cutoffAt);
                boolean available = !cancelled && !pastCutoff && vacancies > 0;
                if (requestedQty > 0 && vacancies < requestedQty) {
                    available = false;
                }

                String availabilityId =
                        OctoAvailabilityIdCodec.encode(
                                request.getProductId(), optionId, date, normalizedTime);

                result.add(
                        OctoDTO.Availability.builder()
                                .id(availabilityId)
                                .localDateTimeStart(
                                        OctoAvailabilityIdCodec.toOctoLocalDateTime(
                                                date, normalizedTime))
                                .localDateTimeEnd(
                                        OctoAvailabilityIdCodec.toOctoLocalDateTimeEnd(
                                                date, normalizedTime, event.getDuration()))
                                .utcCutoffAt(formatUtc(cutoffAt))
                                .allDay(false)
                                .available(available)
                                .status(available ? "AVAILABLE" : "SOLD_OUT")
                                .vacancies(vacancies)
                                .capacity(capacity)
                                .maxUnits(vacancies)
                                .openingHours(List.of())
                                .build());
            }
        }
        return result;
    }

    /**
     * Booking must be confirmed by this UTC time. Uses the event min activity threshold when set;
     * otherwise the slot start time.
     */
    private static ZonedDateTime resolveUtcCutoffAt(
            Events event, LocalDate date, String normalizedTime, ZoneId zone) {
        LocalTime localTime = LocalTime.parse(normalizedTime);
        ZonedDateTime eventStart = ZonedDateTime.of(date, localTime, zone);

        if (ActivityThresholdUtil.isConfigured(event.getMinActivityHourThreshold())) {
            return eventStart.minusHours(event.getMinActivityHourThreshold());
        }
        if (ActivityThresholdUtil.isConfigured(event.getMinActivityDayThreshold())) {
            return date.minusDays(event.getMinActivityDayThreshold()).atStartOfDay(zone);
        }
        return eventStart;
    }

    private static String formatUtc(ZonedDateTime time) {
        return time.withZoneSameInstant(ZoneOffset.UTC).format(UTC_FMT);
    }

    private Map<String, List<String>> loadTimesByWeekday(Long eventId) {
        Map<String, List<String>> timesByWeekday = new HashMap<>();
        for (EventDaySchedules schedule :
                eventDaySchedulesRepository.findByEventIdOrderByDayAndTime(eventId)) {
            String day = schedule.getDay() != null ? schedule.getDay().toUpperCase() : null;
            String startTime = schedule.getId() != null ? schedule.getId().getStartTime() : null;
            if (day == null || startTime == null || startTime.isBlank()) {
                continue;
            }
            timesByWeekday
                    .computeIfAbsent(day, ignored -> new ArrayList<>())
                    .add(startTime);
        }
        return timesByWeekday;
    }

    private Set<String> loadCancelledSlots(Long eventId, LocalDate start, LocalDate end) {
        Set<String> cancelled = new HashSet<>();
        for (Object[] row :
                eventTimeSlotExceptionsRepository.findExceptionSlotsByEventIdAndDateRange(
                        eventId, start, end)) {
            LocalDate date = toLocalDate(row[0]);
            String time = row[1] != null ? row[1].toString() : null;
            if (date != null && time != null) {
                cancelled.add(slotKey(date, time));
            }
        }
        return cancelled;
    }

    private static boolean isEventActiveOnDate(Events event, LocalDate date) {
        if (event.getStartDate() != null && date.isBefore(event.getStartDate())) {
            return false;
        }
        if (event.getEndDate() != null && date.isAfter(event.getEndDate())) {
            return false;
        }
        return true;
    }

    private static String slotKey(LocalDate date, String time) {
        return date + "|" + EventSlotReservationService.timeKey(time);
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return new Date(utilDate.getTime()).toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private static int requestedQuantity(List<OctoDTO.AvailabilityUnitRequest> units) {
        if (units == null) {
            return 0;
        }
        return units.stream()
                .filter(u -> u.getQuantity() != null && u.getQuantity() > 0)
                .mapToInt(OctoDTO.AvailabilityUnitRequest::getQuantity)
                .sum();
    }

    private static LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw OctoException.badRequest("INVALID_DATE", field + " is required");
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw OctoException.badRequest("INVALID_DATE", "Invalid " + field);
        }
    }
}
