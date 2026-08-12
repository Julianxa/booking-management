package com.example.service;

import com.example.config.AppProperties;
import com.example.constant.Enums;
import com.example.model.dto.CreateEventResponseDTO;
import com.example.model.dto.CreateTicketTypeResponseDTO;
import com.example.model.dto.EventAvailabilityDTO;
import com.example.model.dto.TicketPricePeriodDTO;
import com.example.model.entity.Events;
import com.example.utils.OctoAvailabilityIdCodec;
import com.example.exception.octo.OctoException;
import com.example.model.dto.OctoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OctoAvailabilityService {

    private final AppProperties appProperties;
    private final OctoCatalogService octoCatalogService;
    private final EventService eventService;
    private final EventSlotReservationService eventSlotReservationService;

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
        LocalDate end =
                request.getLocalDateEnd() == null || request.getLocalDateEnd().isBlank()
                        ? start
                        : parseDate(request.getLocalDateEnd(), "localDateEnd");
        if (end.isBefore(start)) {
            throw OctoException.badRequest("INVALID_DATE_RANGE", "localDateEnd before localDateStart");
        }

        List<CreateTicketTypeResponseDTO> ticketTypes =
                eventService.getTicketTypesByEventId(request.getProductId());
        int requestedQty = requestedQuantity(request.getUnits());

        List<OctoDTO.Availability> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            EventAvailabilityDTO dayAvailability =
                    eventService.getAvailability(true, request.getProductId(), date);
            List<CreateEventResponseDTO.OccupancyDTO> slots =
                    dayAvailability.getOccupancy() != null
                            ? dayAvailability.getOccupancy()
                            : List.of();

            for (CreateEventResponseDTO.OccupancyDTO slot : slots) {
                if (slot.getStatus() == Enums.OccupancyStatus.CANCELLED) {
                    continue;
                }
                String time = OctoAvailabilityIdCodec.normalizeTime(slot.getEventTime());
                int reserved =
                        eventSlotReservationService.getReservedQty(event.getId(), date, time);
                int capacity = event.getMaxCapacity() != null ? event.getMaxCapacity() : 0;
                int vacancies = Math.max(0, capacity - reserved);
                boolean available =
                        slot.getStatus() == Enums.OccupancyStatus.AVAILABLE && vacancies > 0;
                if (requestedQty > 0 && vacancies < requestedQty) {
                    available = false;
                }

                String availabilityId =
                        OctoAvailabilityIdCodec.encode(
                                request.getProductId(), optionId, date, time);

                result.add(
                        OctoDTO.Availability.builder()
                                .id(availabilityId)
                                .localDateTimeStart(
                                        OctoAvailabilityIdCodec.toOctoLocalDateTime(date, time))
                                .localDateTimeEnd(
                                        OctoAvailabilityIdCodec.toOctoLocalDateTimeEnd(
                                                date, time, event.getDuration()))
                                .allDay(false)
                                .available(available)
                                .status(available ? "AVAILABLE" : "SOLD_OUT")
                                .vacancies(vacancies)
                                .capacity(capacity)
                                .maxUnits(vacancies)
                                .unitPricing(buildUnitPricing(ticketTypes))
                                .build());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<OctoDTO.AvailabilityCalendarDay> calendar(
            OctoDTO.AvailabilityCalendarRequest request) {
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            throw OctoException.badRequest("INVALID_PRODUCT_ID", "productId is required");
        }
        octoCatalogService.assertOptionId(request.getOptionId());
        Events event = octoCatalogService.requirePublishedEvent(request.getProductId());

        LocalDate start = parseDate(request.getLocalDateStart(), "localDateStart");
        LocalDate end = parseDate(request.getLocalDateEnd(), "localDateEnd");

        List<OctoDTO.AvailabilityCalendarDay> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            EventAvailabilityDTO dayAvailability =
                    eventService.getAvailability(true, request.getProductId(), date);
            List<CreateEventResponseDTO.OccupancyDTO> slots =
                    dayAvailability.getOccupancy() != null
                            ? dayAvailability.getOccupancy()
                            : List.of();

            int bestVacancies = 0;
            boolean anyAvailable = false;
            for (CreateEventResponseDTO.OccupancyDTO slot : slots) {
                if (slot.getStatus() != Enums.OccupancyStatus.AVAILABLE) {
                    continue;
                }
                String time = OctoAvailabilityIdCodec.normalizeTime(slot.getEventTime());
                int reserved =
                        eventSlotReservationService.getReservedQty(event.getId(), date, time);
                int capacity = event.getMaxCapacity() != null ? event.getMaxCapacity() : 0;
                int vacancies = Math.max(0, capacity - reserved);
                bestVacancies = Math.max(bestVacancies, vacancies);
                if (vacancies > 0) {
                    anyAvailable = true;
                }
            }
            int capacity = event.getMaxCapacity() != null ? event.getMaxCapacity() : 0;
            days.add(
                    OctoDTO.AvailabilityCalendarDay.builder()
                            .localDate(date.toString())
                            .available(anyAvailable)
                            .status(anyAvailable ? "AVAILABLE" : "SOLD_OUT")
                            .vacancies(bestVacancies)
                            .capacity(capacity)
                            .openings(bestVacancies)
                            .build());
        }
        return days;
    }

    private List<OctoDTO.UnitPricing> buildUnitPricing(List<CreateTicketTypeResponseDTO> ticketTypes) {
        List<OctoDTO.UnitPricing> pricing = new ArrayList<>();
        for (CreateTicketTypeResponseDTO ticketType : ticketTypes) {
            if (ticketType.getStatus() != null && ticketType.getStatus() != Enums.TicketTypeStatus.OPEN) {
                continue;
            }
            BigDecimal price = BigDecimal.ZERO;
            if (ticketType.getPeriods() != null && !ticketType.getPeriods().isEmpty()) {
                TicketPricePeriodDTO period = ticketType.getPeriods().get(0);
                if (period.getPrice() != null) {
                    price = period.getPrice();
                }
            }
            pricing.add(
                    OctoDTO.UnitPricing.builder()
                            .unitId(ticketType.getId())
                            .pricing(octoCatalogService.toPricing(price))
                            .build());
        }
        return pricing;
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
