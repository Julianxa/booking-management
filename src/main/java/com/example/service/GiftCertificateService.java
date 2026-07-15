package com.example.service;

import com.example.constant.Enums;
import com.example.converter.BookingItemsConverter;
import com.example.converter.GiftCertificateItemsConverter;
import com.example.exception.booking.BookingEventNotFoundException;
import com.example.exception.event.EventNotFoundException;
import com.example.exception.general.MissingRequiredFieldException;
import com.example.exception.giftCertificate.*;
import com.example.exception.ticket.TicketPricePeriodNotFoundException;
import com.example.exception.ticket.TicketTypeNotFoundException;
import com.example.exception.user.UserNotFoundException;
import com.example.mapper.GiftCertificateMapper;
import com.example.model.dto.*;
import com.example.model.entity.*;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.*;
import com.example.utils.PartialUpdateUtil;
import com.example.utils.ReferenceNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.constant.Enums.GiftCertificateRedemptionStatus.*;
import static com.example.constant.Enums.GiftCertificateStatus.ACTIVE;
import static com.example.constant.Enums.GiftCertificateType.*;
import static java.lang.Math.min;

@Service
@RequiredArgsConstructor
public class GiftCertificateService {
    private final AuditService auditService;
    private final GiftCertificateMapper giftCertificateMapper;
    private final UsersRepository usersRepository;
    private final TicketPricePeriodsRepository ticketPricePeriodsRepository;
    private final EventsRepository eventsRepository;
    private final GiftCertificatesRepository giftCertificatesRepository;
    private final GiftCertificateItemsRepository giftCertificateItemRepository;
    private final GiftCertificateRedemptionRepository giftCertificateRedemptionRepository;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final TicketTypesRepository ticketTypesRepository;
    private final BookingItemsRepository bookingItemsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingItemsConverter bookingItemsConverter;
    private final GiftCertificateItemsConverter giftCertificateItemsConverter;

    @Transactional
    public CreateGiftCertificatesResponseDTO createCertificate(String userSub, CreateGiftCertificateRequestDTO dto) {
        Users user = usersRepository.findByUserSub(userSub)
                .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", userSub)));

        Long eventId = resolveEventId(dto);
        int certificateCount = resolveCertificateCount(dto);
        boolean personalCertificate = isPersonalCertificate(dto);
        String giftCertificateId = personalCertificate
                ? referenceNoGenerator.generateGiftCertificateReference()
                : null;

        if (!personalCertificate) {
            validatePromoCodeAvailable(dto.getPromoCode());
        }

        List<CreateGiftCertificateResponseDTO> certificates = new ArrayList<>();
        String bundleId = null;
        int perCertificateQuantity = personalCertificate ? 1 : dto.getQuantity();
        for (int index = 0; index < certificateCount; index++) {
            String promoCode = personalCertificate
                    ? referenceNoGenerator.generateGiftCertificatePromoCode()
                    : dto.getPromoCode();
            String refNo = personalCertificate
                    ? giftCertificateId
                    : referenceNoGenerator.generateGiftCertificateReference();
            if (bundleId == null) {
                bundleId = refNo;
            }

            GiftCertificates gc = buildGiftCertificate(
                    user.getId(),
                    eventId,
                    dto,
                    perCertificateQuantity,
                    refNo,
                    promoCode);
            addCertificateItems(gc, dto);

            gc = giftCertificatesRepository.save(gc);

            auditService.record("CREATE_CERTIFICATE",
                    GiftCertificates.class.getName(),
                    gc.getId(),
                    user.getId(),
                    "Create gift certificate successfully");

            certificates.add(buildResponse(gc, user.getRefNo(), dto.getEventId()));
        }

        String message = certificateCount == 1
                ? "Gift certificate created successfully"
                : String.format("%d gift certificates created successfully", certificateCount);

        return toCertificatesResponse(bundleId, certificates, message);
    }

    private Long resolveEventId(CreateGiftCertificateRequestDTO dto) {
        if (dto.getType() == EVENT || dto.getType() == PERSONAL_EVENT) {
            if (dto.getEventId() == null || dto.getEventId().isBlank()) {
                throw new MissingRequiredFieldException("event_id is required for EVENT and PERSONAL_EVENT gift certificates");
            }
            return eventsRepository.findIdByRefNo(dto.getEventId())
                    .orElseThrow(() -> new EventNotFoundException(
                            String.format("Event %s not found", dto.getEventId())));
        }

        if (dto.getEventId() == null || dto.getEventId().isBlank()) {
            return null;
        }
        return eventsRepository.findIdByRefNo(dto.getEventId())
                .orElseThrow(() -> new EventNotFoundException(
                        String.format("Event %s not found", dto.getEventId())));
    }

    private int resolveCertificateCount(CreateGiftCertificateRequestDTO dto) {
        if (isPersonalCertificate(dto)) {
            return dto.getQuantity();
        }
        return 1;
    }

    private boolean isPersonalCertificate(CreateGiftCertificateRequestDTO dto) {
        return isPersonalCertificate(dto.getType());
    }

    private void validatePromoCodeAvailable(String promoCode) {
        if (giftCertificatesRepository.existsByPromoCode(promoCode)) {
            throw new GCPromoCodeExistsException(String.format("Promotion code %s already exists", promoCode));
        }
    }

    private void addCertificateItems(GiftCertificates gc, CreateGiftCertificateRequestDTO dto) {
        if (dto.getType() == EVENT || dto.getType() == PERSONAL_EVENT) {
            validateAndAddEventItems(gc, dto.getItems());
        } else if (dto.getType() == PERCENT || dto.getType() == PERSONAL_PERCENT) {
            validateAndAddPercentItems(gc, dto.getItems());
        } else {
            validateAndAddValueItems(gc, dto.getItems());
        }
    }

    @Transactional
    public UpdateGiftCertificatesResponseDTO updateCertificate(String promoCode, UpdateGiftCertificatesRequestDTO dto) {
        GiftCertificates giftCertificates = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new GCNotFoundException(String.format("Gift Certificate with promotion code %s not found", promoCode)));

        PartialUpdateUtil.apply(dto, "expiry_date", dto::getExpiryDate, giftCertificates::setExpiryDate);
        PartialUpdateUtil.apply(dto, "message_to_recipient", dto::getMessageToRecipient, giftCertificates::setMessageToRecipient);
        PartialUpdateUtil.apply(dto, "effective_date", dto::getEffectiveDate, giftCertificates::setEffectiveDate);
        giftCertificates = giftCertificatesRepository.save(giftCertificates);

        auditService.record("UPDATE_CERTIFICATE",
                GiftCertificates.class.getName(),
                giftCertificates.getId(),
                null,
                "Update gift certificate successfully"
        );

        String userRefNo = usersRepository.findRefNoById(giftCertificates.getUserId()).orElse(null);
        String eventRefNo = eventsRepository.findRefNoById(giftCertificates.getEventId()).orElse(null);

        return toUpdateCertificatesResponse(giftCertificates.getRefNo(),
                buildResponse(giftCertificates, userRefNo, eventRefNo),
                "Gift Certificate is updated");
    }

    public GiftCertificateApplicationResult getCertificateRedemptionResult(Bookings booking) {
        // If booking has no gift certificate, return empty result
        if (booking.getGiftCertificateId() == null) {
            return new GiftCertificateApplicationResult(null, List.of(), BigDecimal.ZERO);
        }
        
        // Gift certificate ID exists, so it must be found in database
        GiftCertificates giftCertificate = giftCertificatesRepository.findById(booking.getGiftCertificateId())
                .orElseThrow(() -> new GCNotFoundException(
                    String.format("Gift certificate with ID %s referenced by booking not found", booking.getGiftCertificateId())));

        if (giftCertificate.getType() == VALUE
                || giftCertificate.getType() == PERSONAL_VALUE
                || giftCertificate.getType() == PERCENT
                || giftCertificate.getType() == PERSONAL_PERCENT) {
            BigDecimal appliedDiscount = booking.getDiscount() != null
                    ? booking.getDiscount()
                    : BigDecimal.ZERO;
            return new GiftCertificateApplicationResult(giftCertificate, List.of(), appliedDiscount);
        } else if (giftCertificate.getType() == EVENT || giftCertificate.getType() == PERSONAL_EVENT) {
            Long bookingEventId = bookingEventsRepository.findIdByBookingIdAndEventId(booking.getId(), giftCertificate.getEventId())
                    .orElseThrow(() -> new BookingEventNotFoundException("Booking event not found"));

            List<BookingItems> bookingItems = bookingItemsRepository.findByBookingEventId(bookingEventId);

            List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTicketDTOs = bookingItemsConverter.toTicketTypeDTOs(bookingItems);

            BigDecimal discount = getGiftCertificateDiscount(redeemedTicketDTOs);
            return new GiftCertificateApplicationResult(giftCertificate, redeemedTicketDTOs, discount);

        } else {
            throw new GCNotFoundException(String.format("Unknown gift certificate type for certificate ID %s", giftCertificate.getId()));
        }
    }

    private GiftCertificates buildGiftCertificate(
            Long userId,
            Long eventId,
            CreateGiftCertificateRequestDTO dto,
            int quantity,
            String refNo,
            String promoCode) {
        return GiftCertificates.builder()
                .refNo(refNo)
                .promoCode(promoCode)
                .eventId(eventId)
                .userId(userId)
                .type(dto.getType())
                .effectiveDate(dto.getEffectiveDate())
                .expiryDate(dto.getExpiryDate())
                .quantity(quantity)
                .remainingQuantity(quantity)
                .messageToRecipient(dto.getMessageToRecipient())
                .build();
    }

    private void validateAndAddEventItems(GiftCertificates gc, List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> items) {

        if (items == null || items.isEmpty()) {
            throw new GCItemNotFoundException("Empty ticket list to create EVENT Gift Certificate");
        }

        for (var itemDTO : items) {
            Long ticketTypeId = ticketTypesRepository.findIdByRefNo(itemDTO.getTicketTypeId())
                    .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", itemDTO.getTicketTypeId())));

            gc.getItems().add(GiftCertificateItems.builder()
                    .giftCertificates(gc)
                    .ticketTypeId(ticketTypeId)
                    .quantity(itemDTO.getQuantity())
                    .build());
        }
    }

    private void validateAndAddValueItems(GiftCertificates gc, List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> items) {

        if (items == null || items.isEmpty()) {
            throw new GCItemNotFoundException("Empty item to create VALUE Gift Certificate");
        } else {
            for (var itemDTO : items) {
                if (itemDTO.getValue() == null || itemDTO.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new MissingRequiredFieldException("value must be greater than 0 for VALUE gift certificates");
                }
                gc.getItems().add(GiftCertificateItems.builder()
                        .giftCertificates(gc)
                        .value(itemDTO.getValue())
                        .build());
            }
        }
    }

    private void validateAndAddPercentItems(GiftCertificates gc, List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new GCItemNotFoundException("Empty item to create PERCENT Gift Certificate");
        }
        for (var itemDTO : items) {
            if (itemDTO.getValue() == null
                    || itemDTO.getValue().compareTo(BigDecimal.ZERO) <= 0
                    || itemDTO.getValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new MissingRequiredFieldException(
                        "value must be between 0 (exclusive) and 100 (inclusive) for PERCENT gift certificates");
            }
            gc.getItems().add(GiftCertificateItems.builder()
                    .giftCertificates(gc)
                    .value(itemDTO.getValue())
                    .build());
        }
    }

    public CreateGiftCertificatesResponseDTO getCertificate(String promoCode) {
        GiftCertificates gc = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new GCNotFoundException(String.format("Gift certificate with promotion code %s not found", promoCode)));

        String userRefNo = usersRepository.findRefNoById(gc.getUserId()).orElse(null);
        String eventRefNo = eventsRepository.findRefNoById(gc.getEventId()).orElse(null);

        return toCertificatesResponse(gc.getRefNo(),
                buildResponse(gc, userRefNo, eventRefNo),
                "Gift certificate retrieved successfully");
    }

    public CreateGiftCertificatesResponseDTO getCertificateById(String gcRefNo) {
        List<GiftCertificates> certificates = giftCertificatesRepository.findByRefNoOrderByIdAsc(gcRefNo);
        if (certificates.isEmpty()) {
            throw new GCNotFoundException(String.format("Gift certificate with id %s not found", gcRefNo));
        }

        List<CreateGiftCertificateResponseDTO> certificateDTOs = certificates.stream()
                .map(gc -> {
                    String userRefNo = usersRepository.findRefNoById(gc.getUserId()).orElse(null);
                    String eventRefNo = eventsRepository.findRefNoById(gc.getEventId()).orElse(null);
                    return buildResponse(gc, userRefNo, eventRefNo);
                })
                .toList();

        return toCertificatesResponse(gcRefNo, certificateDTOs, "Gift certificate retrieved successfully");
    }

    public GetListGiftCertificateResponseDTO getGiftCertificates(
            Pageable pageable, String eventRefNo) {
        Long eventId = eventRefNo != null ? eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)))
                : null;

        Page<String> bundleIdsPage = giftCertificatesRepository.findDistinctRefNos(eventId, pageable);
        if (bundleIdsPage.isEmpty()) {
            GetListGiftCertificateResponseDTO emptyResponse =
                    giftCertificateMapper.toGetListResponse(bundleIdsPage, List.of());
            emptyResponse.setMessage("Retrieve list of Gift Certificates successfully.");
            emptyResponse.setTimestamp(ZonedDateTime.now());
            return emptyResponse;
        }

        List<GiftCertificates> certificates =
                giftCertificatesRepository.findByRefNoInOrderByRefNoAscIdAsc(bundleIdsPage.getContent());

        Map<String, List<GiftCertificates>> certificatesByBundleId = certificates.stream()
                .collect(Collectors.groupingBy(GiftCertificates::getRefNo));

        List<CreateGiftCertificatesResponseDTO> content = bundleIdsPage.getContent().stream()
                .map(bundleId -> toBundleResponse(bundleId, certificatesByBundleId.getOrDefault(bundleId, List.of())))
                .toList();

        GetListGiftCertificateResponseDTO getListGiftCertificateResponseDTO =
                giftCertificateMapper.toGetListResponse(bundleIdsPage, content);
        getListGiftCertificateResponseDTO.setMessage("Retrieve list of Gift Certificates successfully.");
        getListGiftCertificateResponseDTO.setTimestamp(ZonedDateTime.now());
        return getListGiftCertificateResponseDTO;
    }

    Enums.GiftCertificateStatus findStatusByCertificate(GiftCertificates gc) {
        if (gc.getCancelledAt() != null) return Enums.GiftCertificateStatus.CANCELLED;
        if (gc.getExpiryDate() != null && gc.getExpiryDate().isBefore(LocalDate.now())) {
            return Enums.GiftCertificateStatus.EXPIRED;
        }
        if (gc.getRemainingQuantity() < 1) return Enums.GiftCertificateStatus.CONSUMED;
        return ACTIVE;
    }

    @Transactional
    public GiftCertificates validateGiftCertificateForBooking(String promoCode, Long userId) {
        GiftCertificates gc = giftCertificatesRepository.findByPromoCodeWithLock(promoCode)
                .orElseThrow(() -> new GCNotFoundException(String.format("Gift certificate with promotion code %s not found", promoCode)));

        if (gc.isCancelled()) throw new InvalidGCException("The gift certificate has been cancelled");
        if (gc.getRemainingQuantity() < 1) throw new InvalidGCException("The gift certificate already redeemed");
        if (gc.isExpired()) throw new InvalidGCException("The gift certificate has expired");
        if (!gc.isEffective()) throw new InvalidGCException("This gift certificate is not effective");
        return gc;
    }

    @Transactional
    private GiftCertificateApplicationResult applyValueType(GiftCertificates gc, BigDecimal grandTotal) {
        GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(gc.getId())
                .orElseThrow(() -> new GCItemNotFoundException(String.format("Value gift certificate item not found with %s", gc.getId())));

        BigDecimal cappedDiscount = item.getValue().min(grandTotal);
        return new GiftCertificateApplicationResult(gc, List.of(), cappedDiscount);
    }

    private GiftCertificateApplicationResult applyPercentType(GiftCertificates gc, BigDecimal grandTotal) {
        GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(gc.getId())
                .orElseThrow(() -> new GCItemNotFoundException(
                        String.format("Percent gift certificate item not found with %s", gc.getId())));

        BigDecimal percent = item.getValue();
        // Example: payment 100 with 90% GC => discount 90, final paid 10
        BigDecimal discount = grandTotal
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (discount.compareTo(grandTotal) > 0) {
            discount = grandTotal;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        return new GiftCertificateApplicationResult(gc, List.of(), discount);
    }

    @Transactional
    private GiftCertificateApplicationResult applyEventType(
            GiftCertificates gc,
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs,
            BigDecimal grandTotal) {
        List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets = new ArrayList<>();

        for (CreateBookingRequestDTO.BookingEventDTO bookingEventDTO : bookingEventDTOs) {
            Long eventId = eventsRepository.findIdByRefNo(bookingEventDTO.getEvent().getId())
                    .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found",  bookingEventDTO.getEvent().getId())));

            if (gc.getEventId() != null && !gc.getEventId().equals(eventId)) {
                continue;
            }

            List<GiftCertificateItems> gcItems = giftCertificateItemRepository.getEventCertByGiftCertificateId(gc.getId())
                    .orElseThrow(() -> new GCItemNotFoundException(String.format("Gift Certificate items not found with %s", gc.getId())));

            for (CreateBookingRequestDTO.TicketTypeDTO ticketDTO : bookingEventDTO.getTickets()) {
                for (GiftCertificateItems gcItem : gcItems) {
                    Long ticketTypeId = ticketTypesRepository.findIdByRefNo(ticketDTO.getId())
                            .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", ticketDTO.getId())));

                    if (ticketTypeId.equals(gcItem.getTicketTypeId())) {
                        int redeemedQty = min(ticketDTO.getQuantity(), gcItem.getQuantity());

                        redeemedTickets.add(CreateBookingRequestDTO.TicketTypeDTO.builder()
                                .id(ticketDTO.getId())
                                .name(ticketDTO.getName())
                                .quantity(redeemedQty)
                                .build());
                    }
                }
            }
        }

        BigDecimal discount = getGiftCertificateDiscount(redeemedTickets).min(grandTotal);

        return new GiftCertificateApplicationResult(gc, redeemedTickets, discount);
    }

    private CreateGiftCertificateResponseDTO buildResponse(GiftCertificates gc, String userRefNo, String eventRefNo) {
        CreateGiftCertificateResponseDTO response = giftCertificateMapper.toCreateResponseDTO(
                userRefNo, eventRefNo, gc, giftCertificateItemsConverter.toGiftCertificateItemDTOs(gc));
        response.setStatus(findStatusByCertificate(gc));

        return response;
    }

    private CreateGiftCertificatesResponseDTO toCertificatesResponse(
            String bundleId,
            CreateGiftCertificateResponseDTO certificate,
            String message) {
        return toCertificatesResponse(bundleId, List.of(certificate), message);
    }

    private CreateGiftCertificatesResponseDTO toCertificatesResponse(
            String bundleId,
            List<CreateGiftCertificateResponseDTO> certificates,
            String message) {
        return CreateGiftCertificatesResponseDTO.builder()
                .id(bundleId)
                .certificates(certificates)
                .message(message)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    private UpdateGiftCertificatesResponseDTO toUpdateCertificatesResponse(
            String bundleId,
            CreateGiftCertificateResponseDTO certificate,
            String message) {
        return toUpdateCertificatesResponse(bundleId, List.of(certificate), message);
    }

    private UpdateGiftCertificatesResponseDTO toUpdateCertificatesResponse(
            String bundleId,
            List<CreateGiftCertificateResponseDTO> certificates,
            String message) {
        return UpdateGiftCertificatesResponseDTO.builder()
                .id(bundleId)
                .certificates(certificates)
                .message(message)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    private CreateGiftCertificatesResponseDTO toBundleResponse(String bundleId, List<GiftCertificates> certificates) {
        List<CreateGiftCertificateResponseDTO> certificateDTOs = certificates.stream()
                .map(gc -> {
                    String userRefNo = usersRepository.findRefNoById(gc.getUserId())
                            .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", gc.getUserId())));
                    String eventRefNo = eventsRepository.findRefNoById(gc.getEventId()).orElse(null);
                    return buildResponse(gc, userRefNo, eventRefNo);
                })
                .toList();

        return CreateGiftCertificatesResponseDTO.builder()
                .id(bundleId)
                .certificates(certificateDTOs)
                .build();
    }

    private boolean isPersonalCertificate(Enums.GiftCertificateType type) {
        return type == PERSONAL_EVENT || type == PERSONAL_VALUE || type == PERSONAL_PERCENT;
    }

    public BigDecimal getGiftCertificateDiscount(List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets) {
        return redeemedTickets.stream()
                .map(this::calculateTicketSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTicketSubtotal(CreateBookingRequestDTO.TicketTypeDTO ticket) {
        Long ticketTypeId = ticketTypesRepository.findIdByRefNo(ticket.getId())
                .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", ticket.getId())));

        BigDecimal price = ticketPricePeriodsRepository.findActivePrice(ticketTypeId, null)
                .orElseThrow(() -> new TicketPricePeriodNotFoundException(String.format("Price period not found with ticket type %s", ticketTypeId)))
                .getPrice();

        return price.multiply(BigDecimal.valueOf(ticket.getQuantity()));
    }

    public UpdateGiftCertificatesResponseDTO updateGiftCertificateStatus(String promoCode, UpdateGiftCertificatesStatusRequestDTO dto) {
        GiftCertificates giftCertificates = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new GCNotFoundException(String.format("Gift Certificate not found with promotion code %s", promoCode)));

        ZonedDateTime actionAt = ZonedDateTime.now();
        String message;
        if (dto.getStatus() == Enums.GiftCertificateStatus.CANCELLED) {
            giftCertificates.setCancelledAt(actionAt);
            giftCertificatesRepository.save(giftCertificates);
            message = "Gift Certificate closed successfully";
        } else if (dto.getStatus() == ACTIVE) {
            giftCertificates.setCancelledAt(null);
            giftCertificatesRepository.save(giftCertificates);
            message = "Gift Certificate opened successfully";
        } else {
            throw new IllegalArgumentException("Invalid EventStatus: " + dto.getStatus() +
                    ". Allowed values are: OPEN, CLOSE");
        }

        String userRefNo = usersRepository.findRefNoById(giftCertificates.getUserId()).orElse(null);
        String eventRefNo = eventsRepository.findRefNoById(giftCertificates.getEventId()).orElse(null);

        return toUpdateCertificatesResponse(giftCertificates.getRefNo(),
                buildResponse(giftCertificates, userRefNo, eventRefNo),
                message);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public GiftCertificateApplicationResult validateAndCalculateGiftCertificate(
            Users loggedInUser,
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs,
            String promoCode,
            BigDecimal grandTotal) {
        if (promoCode == null) {
            return new GiftCertificateApplicationResult(null, List.of(), BigDecimal.ZERO);
        }

        Long userId = loggedInUser != null ? loggedInUser.getId() : null;

        GiftCertificates gc = validateGiftCertificateForBooking(promoCode, userId);

        BigDecimal bookingTotal = grandTotal != null ? grandTotal : BigDecimal.ZERO;

        GiftCertificateApplicationResult result;
        if (gc.getType() == VALUE || gc.getType() == PERSONAL_VALUE) {
            result = applyValueType(gc, bookingTotal);
        } else if (gc.getType() == PERCENT || gc.getType() == PERSONAL_PERCENT) {
            result = applyPercentType(gc, bookingTotal);
        } else if (gc.getType() == EVENT || gc.getType() == PERSONAL_EVENT) {
            result = applyEventType(gc, bookingEventDTOs, bookingTotal);
        } else {
            throw new InvalidGCException(
                    String.format("Unsupported gift certificate type: %s", gc.getType()));
        }

        if (result.discount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidGCException("Gift certificate does not apply to this booking");
        }

        return result;
    }

    @Transactional
    public void preserveGiftCertificate(Users user, Bookings booking, GiftCertificateApplicationResult giftCertificateApplicationResult) {
        Long userId = user != null ? user.getId() : null;

        GiftCertificates gc = giftCertificateApplicationResult.certificate();
        if (gc == null) {
            return;
        }
        if (gc.getRemainingQuantity() < 1) {
            throw new InvalidGCException("The gift certificate already redeemed");
        }

        BigDecimal discount = giftCertificateApplicationResult.discount();
        if (gc.getType() == VALUE || gc.getType() == PERSONAL_VALUE) {
            GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(gc.getId())
                    .orElseThrow(() -> new GCItemNotFoundException(String.format("Value gift certificate item not found with %s", gc.getId())));
            BigDecimal newBalance = item.getValue().subtract(discount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidGCException("Gift certificate balance is insufficient");
            }
            item.setValue(newBalance);
            giftCertificateItemRepository.save(item);
        }

        gc.setRemainingQuantity(gc.getRemainingQuantity() - 1);
        giftCertificatesRepository.save(gc);

        GiftCertificateRedemptions redemption = new GiftCertificateRedemptions();
        redemption.setGiftCertificateId(gc.getId());
        redemption.setBookingId(booking.getId());
        redemption.setRedeemedByUserId(userId);
        redemption.setQuantityUsed(1);
        redemption.setStatus(PENDING);
        giftCertificateRedemptionRepository.save(redemption);

        auditService.record("PRESERVE_GC",
                GiftCertificates.class.getName(),
                gc.getId(),
                user != null ? user.getId() : null,
                gc.getRefNo()
        );
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public GiftCertificateApplicationResult confirmCertificateRedemption(Bookings booking, GiftCertificates giftCertificate, Long userId) {
        if (giftCertificate == null) {
            return null;
        }

        GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingIdWithLock(booking.getId())
                .orElseThrow(() -> new GCRedemptionNotFoundException("Gift certificate redemption not found"));
        if (redemption.getStatus() == SUCCESS) {
            return getCertificateRedemptionResult(booking);
        }
        if (redemption.getStatus() != PENDING) {
            throw new InvalidGCException("Gift certificate redemption is no longer pending");
        }

        redemption.setStatus(SUCCESS);
        redemption.setRedeemedAt(ZonedDateTime.now());
        giftCertificateRedemptionRepository.save(redemption);

        return getCertificateRedemptionResult(booking);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelCertificateRedemption(Bookings booking) {
        GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingIdWithLock(booking.getId())
                .orElse(null);
        if(redemption == null || redemption.getStatus() != PENDING) {
            return;
        }

        redemption.setStatus(FAILED);
        giftCertificateRedemptionRepository.save(redemption);

        GiftCertificates gc = giftCertificatesRepository.findByIdWithLock(redemption.getGiftCertificateId())
                .orElseThrow(() -> new GCNotFoundException("Gift certificate not found"));
        gc.setRemainingQuantity(gc.getRemainingQuantity() + redemption.getQuantityUsed());

        if (gc.getType() == VALUE || gc.getType() == PERSONAL_VALUE) {
            BigDecimal discount = booking.getDiscount() != null ? booking.getDiscount() : BigDecimal.ZERO;
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(gc.getId())
                        .orElseThrow(() -> new GCItemNotFoundException(String.format("Value gift certificate item not found with %s", gc.getId())));
                item.setValue(item.getValue().add(discount));
                giftCertificateItemRepository.save(item);
            }
        }

        giftCertificatesRepository.save(gc);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void reopenCancelledRedemption(Bookings booking) {
        if (booking.getGiftCertificateId() == null) {
            return;
        }

        GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingIdWithLock(booking.getId())
                .orElse(null);
        if (redemption == null || redemption.getStatus() != FAILED) {
            return;
        }

        GiftCertificates gc = giftCertificatesRepository.findByIdWithLock(redemption.getGiftCertificateId())
                .orElseThrow(() -> new GCNotFoundException("Gift certificate not found"));
        if (gc.getRemainingQuantity() < 1) {
            throw new InvalidGCException("The gift certificate is no longer available for this booking");
        }

        BigDecimal discount = booking.getDiscount() != null ? booking.getDiscount() : BigDecimal.ZERO;
        if (gc.getType() == VALUE || gc.getType() == PERSONAL_VALUE) {
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(gc.getId())
                        .orElseThrow(() -> new GCItemNotFoundException(
                                String.format("Value gift certificate item not found with %s", gc.getId())));
                BigDecimal newBalance = item.getValue().subtract(discount);
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvalidGCException("Gift certificate balance is insufficient");
                }
                item.setValue(newBalance);
                giftCertificateItemRepository.save(item);
            }
        }

        gc.setRemainingQuantity(gc.getRemainingQuantity() - 1);
        giftCertificatesRepository.save(gc);

        redemption.setStatus(PENDING);
        giftCertificateRedemptionRepository.save(redemption);
    }
}