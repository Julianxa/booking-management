package com.example.service;

import com.example.constant.Enums;
import com.example.converter.BookingItemsConverter;
import com.example.converter.GiftCertificateItemsConverter;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.GiftCertificateMapper;
import com.example.model.dto.*;
import com.example.model.entity.*;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.*;
import com.example.utils.ReferenceNoGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.constant.Enums.GiftCertificateStatus.ACTIVE;
import static com.example.constant.Enums.GiftCertificateType.EVENT;
import static com.example.constant.Enums.GiftCertificateType.VALUE;
import static java.lang.Math.min;

@Service
@RequiredArgsConstructor
public class GiftCertificateService {
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
    public CreateGiftCertificateResponseDTO createCertificate(String userSub, CreateGiftCertificateRequestDTO dto)
            throws BadRequestException, SQLException {

        Users user = usersRepository.findByUserSub(userSub)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long eventId = eventsRepository.findIdByRefNo(dto.getEventId()).orElse(null);
        GiftCertificates gc = buildGiftCertificate(user.getId(), eventId, dto);

        if (giftCertificatesRepository.existsByPromoCode(gc.getPromoCode())) {
            throw new BadRequestException("Promotion code already exists: " + gc.getPromoCode());
        }

        if (dto.getType() == EVENT) {
            validateAndAddEventItems(gc, dto.getItems());
        } else {
            validateAndAddValueItems(gc, dto.getItems());
        }

        gc = giftCertificatesRepository.save(gc);

        return buildResponse(gc, user.getRefNo(), dto.getEventId());
    }

    @Transactional
    public UpdateGiftCertificateResponseDTO updateCertificate(String promoCode, UpdateGiftCertificateRequestDTO dto) {
        GiftCertificates giftCertificates = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Gift Certificate not found"));

        if (dto.getExpiryDate() != null) {
            giftCertificates.setExpiryDate(dto.getExpiryDate());
        }
        if (dto.getMessageToRecipient() != null) {
            giftCertificates.setMessageToRecipient(dto.getMessageToRecipient());
        }
        if (dto.getEffectiveDate() != null) {
            giftCertificates.setEffectiveDate(dto.getEffectiveDate());
        }
        giftCertificatesRepository.save(giftCertificates);

        UpdateGiftCertificateResponseDTO updateGiftCertificateResponseDTO = new UpdateGiftCertificateResponseDTO();
        updateGiftCertificateResponseDTO.setId(giftCertificates.getRefNo());
        updateGiftCertificateResponseDTO.setPromoCode(giftCertificates.getPromoCode());
        updateGiftCertificateResponseDTO.setEffectiveDate(giftCertificates.getEffectiveDate());
        updateGiftCertificateResponseDTO.setExpiryDate(giftCertificates.getExpiryDate());
        updateGiftCertificateResponseDTO.setUpdatedAt(giftCertificates.getUpdatedAt());
        updateGiftCertificateResponseDTO.setMessage("Gift Certificate is updated");
        updateGiftCertificateResponseDTO.setTimestamp(LocalDateTime.now());
        return updateGiftCertificateResponseDTO;
    }

    public GiftCertificateApplicationResult getCertificateRedemptionResult(Bookings booking) {
        GiftCertificates giftCertificate = giftCertificatesRepository.findById(booking.getGiftCertificateId()).orElse(null);

        if (giftCertificate != null && giftCertificate.getType() == VALUE) {
            GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(giftCertificate.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Value gift certificate item not found"));

            return new GiftCertificateApplicationResult(giftCertificate, List.of(), item.getValue());
        } else if (giftCertificate != null && giftCertificate.getType() == EVENT) {
            Long bookingEventId = bookingEventsRepository.findIdByBookingIdAndEventId(booking.getId(), giftCertificate.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking event not found"));

            List<BookingItems> bookingItems = bookingItemsRepository.findByBookingEventId(bookingEventId);

            List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTicketDTOs = bookingItemsConverter.toTicketTypeDTOs(bookingItems);

            BigDecimal discount = getGiftCertificateDiscount(redeemedTicketDTOs);
            return new GiftCertificateApplicationResult(giftCertificate, redeemedTicketDTOs, discount);

        } else {
            return null;
        }
    }

    private GiftCertificates buildGiftCertificate(Long userId, Long eventId, CreateGiftCertificateRequestDTO dto) throws SQLException {
        return GiftCertificates.builder()
                .refNo(referenceNoGenerator.generateGiftCertificateReference())
                .promoCode(dto.getPromoCode())
                .eventId(eventId)
                .userId(userId)
                .type(dto.getType())
                .effectiveDate(dto.getEffectiveDate())
                .expiryDate(dto.getExpiryDate())
                .quantity(dto.getQuantity())
                .remainingQuantity(dto.getQuantity())
                .messageToRecipient(dto.getMessageToRecipient())
                .build();
    }

    private void validateAndAddEventItems(GiftCertificates gc, List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> items)
            throws BadRequestException {

        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Empty ticket list to create EVENT Gift Certificate");
        }

        for (var itemDTO : items) {
            Long ticketTypeId = ticketTypesRepository.findIdByRefNo(itemDTO.getTicketTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found: " + itemDTO.getTicketTypeId()));

            gc.getItems().add(GiftCertificateItems.builder()
                    .giftCertificates(gc)
                    .ticketTypeId(ticketTypeId)
                    .quantity(itemDTO.getQuantity())
                    .build());
        }
    }

    private void validateAndAddValueItems(GiftCertificates gc, List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> items) throws BadRequestException {

        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Empty item to create VALUE Gift Certificate");
        } else {
            for (var itemDTO : items) {
                gc.getItems().add(GiftCertificateItems.builder()
                        .giftCertificates(gc)
                        .value(itemDTO.getValue())
                        .build());
            }
        }
    }

    public CreateGiftCertificateResponseDTO getCertificate(String promoCode) {
        GiftCertificates gc = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Gift certificate not found: " + promoCode));

        String userRefNo = usersRepository.findRefNoById(gc.getUserId()).orElse(null);
        String eventRefNo = eventsRepository.findRefNoById(gc.getEventId()).orElse(null);

        CreateGiftCertificateResponseDTO response = giftCertificateMapper.toCreateResponseDTO(
                userRefNo, eventRefNo, gc, giftCertificateItemsConverter.toGiftCertificateItemDTOs(gc));

        response.setStatus(findStatusByCertificate(gc));
        return response;
    }

    public GetListGiftCertificateResponseDTO getGiftCertificates(
            Pageable pageable, String eventRefNo) {
        Page<GiftCertificates> giftCertificatesPage;
        Long eventId = eventRefNo != null ? eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"))
                : null;
        if (eventId != null) {
            giftCertificatesPage = giftCertificatesRepository.findByEventId(eventId, pageable);
        } else {
            giftCertificatesPage = giftCertificatesRepository.findAll(pageable);
        }

        List<CreateGiftCertificateResponseDTO> content = giftCertificatesPage.getContent().stream()
                .map(giftCertificate -> {
                    String userRefNo = usersRepository.findRefNoById(giftCertificate.getUserId())
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    String evtRefNo = eventsRepository.findRefNoById(giftCertificate.getEventId())
                            .orElse(null);

                    return buildResponse(giftCertificate, userRefNo, evtRefNo);
                })
                .toList();

        GetListGiftCertificateResponseDTO getListGiftCertificateResponseDTO = giftCertificateMapper.toGetListResponse(giftCertificatesPage, content);
        getListGiftCertificateResponseDTO.setMessage("Retrieve list of Gift Certificates successfully.");
        getListGiftCertificateResponseDTO.setTimestamp(LocalDateTime.now());
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
    public GiftCertificates validateGiftCertificateForBooking(String promoCode, Long userId) throws BadRequestException {
        GiftCertificates gc = giftCertificatesRepository.findByPromoCodeWithLock(promoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Gift certificate not found: " + promoCode));

        if (gc.isCancelled()) throw new BadRequestException("The gift certificate has been cancelled");
        if (gc.getRemainingQuantity() < 1) throw new BadRequestException("The gift certificate already redeemed");
        if (gc.isExpired()) throw new BadRequestException("The gift certificate has expired");
        if (!gc.isEffective()) throw new BadRequestException("This gift certificate is not effective");
        return gc;
    }

    public GiftCertificateApplicationResult applyGiftCertificate(
            GiftCertificates gc, List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
        GiftCertificateApplicationResult giftCertificateApplicationResult;
        if (gc.getType() == VALUE) {
            giftCertificateApplicationResult = applyValueType(gc);
        } else {
            giftCertificateApplicationResult = applyEventType(gc, bookingEventDTOs);
        }
        return giftCertificateApplicationResult;
    }

    private GiftCertificateApplicationResult applyValueType(GiftCertificates gc) {
        GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(gc.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Value gift certificate item not found"));

        gc.setRemainingQuantity(gc.getRemainingQuantity() - 1);
        giftCertificatesRepository.save(gc);

        return new GiftCertificateApplicationResult(gc, List.of(), item.getValue());
    }

    private GiftCertificateApplicationResult applyEventType(GiftCertificates gc, List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
        List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets = new ArrayList<>();

        for (CreateBookingRequestDTO.BookingEventDTO bookingEventDTO : bookingEventDTOs) {
            Long eventId = eventsRepository.findIdByRefNo(bookingEventDTO.getEvent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + bookingEventDTO.getEvent().getId()));

            if (gc.getEventId() != null && !gc.getEventId().equals(eventId)) {
                continue;
            }

            List<GiftCertificateItems> gcItems = giftCertificateItemRepository.getEventCertByGiftCertificateId(gc.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gift Certificate items not found"));

            for (CreateBookingRequestDTO.TicketTypeDTO ticketDTO : bookingEventDTO.getTickets()) {
                for (GiftCertificateItems gcItem : gcItems) {
                    Long ticketTypeId = ticketTypesRepository.findIdByRefNo(ticketDTO.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found"));

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

        gc.setRemainingQuantity(gc.getRemainingQuantity() - 1);
        giftCertificatesRepository.save(gc);

        BigDecimal discount = getGiftCertificateDiscount(redeemedTickets);

        return new GiftCertificateApplicationResult(gc, redeemedTickets, discount);
    }

    @Transactional
    public void confirmCertificateRedemption(Bookings booking, GiftCertificates gc, Long userId) {
        GiftCertificateRedemptions giftCertificateRedemptions = new GiftCertificateRedemptions();
        giftCertificateRedemptions.setGiftCertificateId(gc.getId());
        giftCertificateRedemptions.setBookingId(booking.getId());
        giftCertificateRedemptions.setRedeemedByUserId(userId);
        giftCertificateRedemptions.setQuantityUsed(1);
        giftCertificateRedemptions.setRedeemedAt(LocalDateTime.now());
        giftCertificateRedemptionRepository.save(giftCertificateRedemptions);
    }

    private CreateGiftCertificateResponseDTO buildResponse(GiftCertificates gc, String userRefNo, String eventRefNo) {
        CreateGiftCertificateResponseDTO response = giftCertificateMapper.toCreateResponseDTO(
                userRefNo, eventRefNo, gc, giftCertificateItemsConverter.toGiftCertificateItemDTOs(gc));
        response.setStatus(findStatusByCertificate(gc));
        return response;
    }

    public BigDecimal getGiftCertificateDiscount(List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets) {
        return redeemedTickets.stream()
                .map(this::calculateTicketSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTicketSubtotal(CreateBookingRequestDTO.TicketTypeDTO ticket) {
        Long ticketTypeId = ticketTypesRepository.findIdByRefNo(ticket.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found"));

        BigDecimal price = ticketPricePeriodsRepository.findActivePrice(ticketTypeId, null)
                .orElseThrow(() -> new ResourceNotFoundException("Price period not found"))
                .getPrice();

        return price.multiply(BigDecimal.valueOf(ticket.getQuantity()));
    }

    public UpdateGiftCertificateStatusResponseDTO updateGiftCertificateStatus(String promoCode, UpdateGiftCertificateStatusRequestDTO dto) {
        GiftCertificates giftCertificates = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Gift Certificate not found with reference no: " + promoCode));

        LocalDateTime actionAt = LocalDateTime.now();
        UpdateGiftCertificateStatusResponseDTO updateGiftCertificateStatusResponseDTO = new UpdateGiftCertificateStatusResponseDTO();
        if (dto.getStatus() == Enums.GiftCertificateStatus.CANCELLED) {
            giftCertificates.setCancelledAt(actionAt);
            giftCertificatesRepository.save(giftCertificates);
            updateGiftCertificateStatusResponseDTO.setStatus(Enums.GiftCertificateStatus.CANCELLED);
            updateGiftCertificateStatusResponseDTO.setCancelledAt(actionAt);
            updateGiftCertificateStatusResponseDTO.setMessage("Gift Certificate closed successfully");
        } else if (dto.getStatus() == ACTIVE) {
            giftCertificates.setCancelledAt(null);
            giftCertificatesRepository.save(giftCertificates);
            updateGiftCertificateStatusResponseDTO.setStatus(ACTIVE);
            updateGiftCertificateStatusResponseDTO.setMessage("Gift Certificate opened successfully");
        } else {
            throw new IllegalArgumentException("Invalid EventStatus: " + dto.getStatus() +
                    ". Allowed values are: OPEN, CLOSE");
        }

        updateGiftCertificateStatusResponseDTO.setPromoCode(promoCode);
        updateGiftCertificateStatusResponseDTO.setTimestamp(actionAt);
        return updateGiftCertificateStatusResponseDTO;
    }

    @Transactional
    public GiftCertificateApplicationResult reserveGiftCertificate(Users loggedInUser, Bookings booking, List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs, String promoCode) throws BadRequestException {
        if (promoCode == null) {
            return new GiftCertificateApplicationResult(null, List.of(), BigDecimal.ZERO);
        }

        Long userId = loggedInUser != null ? loggedInUser.getId() : null;

        GiftCertificates gc = validateGiftCertificateForBooking(promoCode, userId);

        return applyGiftCertificate(gc, bookingEventDTOs);
    }

    GiftCertificateApplicationResult handleGiftCertificateRedemption(
            Bookings booking, GiftCertificates giftCertificate, Long userId) {

        if (giftCertificate == null) {
            return null;
        }

        confirmCertificateRedemption(booking, giftCertificate, userId);
        return getCertificateRedemptionResult(booking);
    }
}