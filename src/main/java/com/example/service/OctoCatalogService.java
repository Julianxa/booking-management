package com.example.service;

import com.example.config.AppProperties;
import com.example.constant.Enums;
import com.example.model.dto.CreateEventResponseDTO;
import com.example.model.dto.CreateTicketTypeResponseDTO;
import com.example.model.dto.TicketPricePeriodDTO;
import com.example.model.entity.Events;
import com.example.exception.octo.OctoException;
import com.example.model.dto.OctoDTO;
import com.example.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OctoCatalogService {

    private final AppProperties appProperties;
    private final EventService eventService;
    private final EventsRepository eventsRepository;

    @Transactional(readOnly = true)
    public OctoDTO.Supplier getSupplier() {
        AppProperties.Octo octo = appProperties.getOcto();
        return OctoDTO.Supplier.builder()
                .id(octo.getSupplierId())
                .name(octo.getSupplierName())
                .endpoint(trimSlash(appProperties.getBaseUrl()) + "/octo")
                .contact(
                        OctoDTO.Contact.builder()
                                .name(blankToNull(octo.getContactName()))
                                .emailAddress(blankToNull(octo.getContactEmail()))
                                .phoneNumber(blankToNull(octo.getContactTelephone()))
                                .locales(List.of("en", "zh-CN", "zh-HK"))
                                .country("HK")
                                .build())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OctoDTO.Product> getProducts() {
        return eventService
                .getAllEvents(true, PageRequest.of(0, 500), null)
                .getContent()
                .stream()
                .map(event -> toProduct(event.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OctoDTO.Product getProduct(String productId) {
        eventsRepository
                .findByRefNoAndOpenStatusAndPublished(productId)
                .orElseThrow(
                        () ->
                                OctoException.notFound(
                                        "INVALID_PRODUCT_ID", "Product not found: " + productId));
        return toProduct(productId);
    }

    private OctoDTO.Product toProduct(String eventRefNo) {
        CreateEventResponseDTO event = eventService.getEvent(eventRefNo);
        List<CreateTicketTypeResponseDTO> ticketTypes =
                eventService.getTicketTypesByEventId(eventRefNo);

        List<OctoDTO.Unit> units = new ArrayList<>();
        for (CreateTicketTypeResponseDTO ticketType : ticketTypes) {
            if (ticketType.getStatus() != null && ticketType.getStatus() != Enums.TicketTypeStatus.OPEN) {
                continue;
            }
            units.add(
                    OctoDTO.Unit.builder()
                            .id(ticketType.getId())
                            .internalName(ticketType.getName())
                            .type(guessUnitType(ticketType.getName()))
                            .requiredContactFields(List.of("firstName", "lastName", "emailAddress"))
                            .pricingFrom(List.of(toPricing(resolvePrice(ticketType))))
                            .build());
        }

        OctoDTO.Option option =
                OctoDTO.Option.builder()
                        .id(appProperties.getOcto().getDefaultOptionId())
                        .defaultOption(true)
                        .internalName("Default")
                        .restrictions(
                                OctoDTO.OptionRestrictions.builder()
                                        .minUnits(1)
                                        .maxUnits(event.getMaxCapacity())
                                        .build())
                        .units(units)
                        .build();

        return OctoDTO.Product.builder()
                .id(eventRefNo)
                .internalName(event.getName())
                .reference(eventRefNo)
                .locale("en")
                .timeZone(appProperties.getOcto().getTimeZone())
                .allowFreesale(false)
                .instantConfirmation(true)
                .instantDelivery(true)
                .availabilityRequired(true)
                .availabilityType("START_TIME")
                .deliveryFormats(List.of("QRCODE"))
                .deliveryMethods(List.of("VOUCHER", "TICKET"))
                .redemptionMethod("DIGITAL")
                .options(List.of(option))
                .build();
    }

    private BigDecimal resolvePrice(CreateTicketTypeResponseDTO ticketType) {
        if (ticketType.getPeriods() == null || ticketType.getPeriods().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return ticketType.getPeriods().stream()
                .map(TicketPricePeriodDTO::getPrice)
                .filter(p -> p != null)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    public OctoDTO.Pricing toPricing(BigDecimal amount) {
        String currency = appProperties.getOcto().getCurrency();
        int precision = 2;
        int minor =
                amount == null
                        ? 0
                        : amount.movePointRight(precision).setScale(0, RoundingMode.HALF_UP).intValueExact();
        return OctoDTO.Pricing.builder()
                .original(minor)
                .retail(minor)
                .currency(currency)
                .currencyPrecision(precision)
                .build();
    }

    private static String guessUnitType(String name) {
        if (name == null) {
            return "OTHER";
        }
        String lower = name.toLowerCase();
        if (lower.contains("child") || lower.contains("kid")) {
            return "CHILD";
        }
        if (lower.contains("senior") || lower.contains("elder")) {
            return "SENIOR";
        }
        if (lower.contains("infant") || lower.contains("baby")) {
            return "INFANT";
        }
        if (lower.contains("adult")) {
            return "ADULT";
        }
        return "OTHER";
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** Used by availability/booking to validate option id. */
    public void assertOptionId(String optionId) {
        String expected = appProperties.getOcto().getDefaultOptionId();
        if (optionId != null && !optionId.isBlank() && !expected.equals(optionId)) {
            throw OctoException.badRequest("INVALID_OPTION_ID", "Unknown optionId: " + optionId);
        }
    }

    public Events requirePublishedEvent(String productId) {
        return eventsRepository
                .findByRefNoAndOpenStatusAndPublished(productId)
                .orElseThrow(
                        () ->
                                OctoException.notFound(
                                        "INVALID_PRODUCT_ID", "Product not found: " + productId));
    }
}
