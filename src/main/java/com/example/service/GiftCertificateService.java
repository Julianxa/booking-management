package com.example.service;

import com.example.constant.Enums;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Transactional
    public CreateGiftCertificateResponseDTO createCertificate(String userSub, CreateGiftCertificateRequestDTO dto)
            throws BadRequestException, SQLException {

        Users user = usersRepository.findByUserSub(userSub)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        GiftCertificates gc = buildGiftCertificate(user.getId(), dto);

        if(giftCertificatesRepository.existsByPromoCode(gc.getPromoCode())) {
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

        if(dto.getExpiryDate() != null) {giftCertificates.setExpiryDate(dto.getExpiryDate());}
        if(dto.getMessageToRecipient() != null) {giftCertificates.setMessageToRecipient(dto.getMessageToRecipient());}
        giftCertificatesRepository.save(giftCertificates);

        UpdateGiftCertificateResponseDTO updateGiftCertificateResponseDTO = new UpdateGiftCertificateResponseDTO();
        updateGiftCertificateResponseDTO.setId(giftCertificates.getRefNo());
        updateGiftCertificateResponseDTO.setPromoCode(giftCertificates.getPromoCode());
        updateGiftCertificateResponseDTO.setExpiryDate(giftCertificates.getExpiryDate());
        updateGiftCertificateResponseDTO.setUpdatedAt(giftCertificates.getUpdatedAt());
        updateGiftCertificateResponseDTO.setMessage("Gift Certificate is updated");
        updateGiftCertificateResponseDTO.setTimestamp(LocalDateTime.now());
        return updateGiftCertificateResponseDTO;
    }

    private GiftCertificates buildGiftCertificate(Long userId, CreateGiftCertificateRequestDTO dto) throws SQLException {
        return GiftCertificates.builder()
                .refNo(referenceNoGenerator.generateGiftCertificateReference())
                .promoCode(dto.getPromoCode())
                .userId(userId)
                .type(dto.getType())
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

        for (var itemDto : items) {
            Long ticketTypeId = ticketTypesRepository.findIdByRefNo(itemDto.getTicketTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found: " + itemDto.getTicketTypeId()));

            gc.getItems().add(GiftCertificateItems.builder()
                    .giftCertificates(gc)
                    .ticketTypeId(ticketTypeId)
                    .quantity(itemDto.getQuantity())
                    .value(itemDto.getValue())
                    .build());
        }
    }

    private void validateAndAddValueItems(GiftCertificates gc, List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> items) throws BadRequestException {

        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Empty item to create VALUE Gift Certificate");
        } else {
            for (var itemDto : items) {
                gc.getItems().add(GiftCertificateItems.builder()
                        .giftCertificates(gc)
                        .value(itemDto.getValue())
                        .build());
            }
        }
    }

    public CreateGiftCertificateResponseDTO getCertificate(String promoCode) {
        GiftCertificates gc = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Gift certificate not found: " + promoCode));

        String userRefNo = usersRepository.findRefNoById(gc.getUserId()).orElse(null);
        String eventRefNo = eventsRepository.findRefNoById(gc.getEventId()).orElse(null);

        CreateGiftCertificateResponseDTO response = giftCertificateMapper.toResponseDto(
                userRefNo, eventRefNo, gc, mapItems(gc));

        response.setStatus(findStatusByCertificate(gc));
        return response;
    }

    public GetListGiftCertificateResponseDTO getGiftCertificates(
            Pageable pageable, String eventRefNo) {
        Page<GiftCertificates> giftCertificatesPage;
        Long eventId  = eventRefNo != null ? eventsRepository.findIdByRefNo(eventRefNo)
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

    private List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> mapItems(GiftCertificates gc) {
        return gc.getItems().stream()
                .map(this::toItemResponseDto)
                .collect(Collectors.toList());
    }

    Enums.GiftCertificateStatus findStatusByCertificate(GiftCertificates gc) {
        if (gc.getCancelledAt() != null) return Enums.GiftCertificateStatus.CANCELLED;
        if (gc.getExpiryDate() != null && gc.getExpiryDate().isBefore(LocalDate.now())) {
            return Enums.GiftCertificateStatus.EXPIRED;
        }
        if (gc.getRemainingQuantity() < 1) return Enums.GiftCertificateStatus.REDEEMED;
        return ACTIVE;
    }

    public GiftCertificates validateGiftCertificateForBooking(String promoCode, Long userId) throws BadRequestException {
        GiftCertificates gc = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Gift certificate not found: " + promoCode));

        if (!gc.isUsable()) {
            if (gc.isCancelled()) throw new BadRequestException("Gift certificate has been cancelled");
            if (gc.getRemainingQuantity() < 1) throw new BadRequestException("Gift certificate already redeemed");
            if (gc.isExpired()) throw new BadRequestException("Gift certificate has expired");
            throw new BadRequestException("Gift certificate is not usable");
        }
        return gc;
    }

    public GiftCertificateApplicationResult applyGiftCertificateToMultiEventBooking(
            Bookings booking, GiftCertificates gc, CreateBookingRequestDTO request, Long userId) {

        if (gc == null) return null;

        GiftCertificateApplicationResult giftCertificateApplicationResult;
        if (gc.getType() == VALUE) {
            giftCertificateApplicationResult = applyValueType(gc);
        } else {
            giftCertificateApplicationResult = applyEventType(gc, request);
        }
        confirmRedemption(booking, gc, userId);
        return giftCertificateApplicationResult;
    }

    private GiftCertificateApplicationResult applyValueType(GiftCertificates gc) {
        GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(gc.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Value gift certificate item not found"));

        gc.setRemainingQuantity(gc.getRemainingQuantity() - 1);
        giftCertificatesRepository.save(gc);

        return new GiftCertificateApplicationResult(gc, List.of(), item.getValue());
    }

    private GiftCertificateApplicationResult applyEventType(GiftCertificates gc, CreateBookingRequestDTO request) {
        List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets = new ArrayList<>();

        for (CreateBookingRequestDTO.BookingEventDTO eventDto : request.getBookingEvents()) {
            Long eventId = eventsRepository.findIdByRefNo(eventDto.getEvent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventDto.getEvent().getId()));

            if (gc.getEventId() != null && !gc.getEventId().equals(eventId)) {
                continue;
            }

            List<GiftCertificateItems> gcItems = giftCertificateItemRepository.getEventCertByGiftCertificateId(gc.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gift Certificate items not found"));

            for (CreateBookingRequestDTO.TicketTypeDTO ticketDto : eventDto.getTickets()) {
                for (GiftCertificateItems gcItem : gcItems) {
                    Long ticketTypeId = ticketTypesRepository.findIdByRefNo(ticketDto.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found"));

                    if (ticketTypeId.equals(gcItem.getTicketTypeId())) {
                        int redeemedQty = min(ticketDto.getQuantity(), gcItem.getQuantity());

                        redeemedTickets.add(CreateBookingRequestDTO.TicketTypeDTO.builder()
                                .id(ticketDto.getId())
                                .name(ticketDto.getName())
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

    private void confirmRedemption(Bookings booking, GiftCertificates gc, Long userId) {
        GiftCertificateRedemptions giftCertificateRedemptions = new GiftCertificateRedemptions();
        giftCertificateRedemptions.setGiftCertificateId(gc.getId());
        giftCertificateRedemptions.setBookingId(booking.getId());
        giftCertificateRedemptions.setRedeemedByUserId(userId);
        giftCertificateRedemptions.setQuantityUsed(1);
        giftCertificateRedemptions.setRedeemedAt(LocalDateTime.now());
        giftCertificateRedemptionRepository.save(giftCertificateRedemptions);
    }

    private CreateGiftCertificateResponseDTO buildResponse(GiftCertificates gc, String userRefNo, String eventRefNo) {
        List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> itemDtos = gc.getItems().stream()
                .map(this::toItemResponseDto)
                .collect(Collectors.toList());

        CreateGiftCertificateResponseDTO response = giftCertificateMapper.toResponseDto(
                userRefNo, eventRefNo, gc, itemDtos);
        response.setStatus(findStatusByCertificate(gc));
        return response;
    }

    private CreateGiftCertificateRequestDTO.GiftCertificateItemDTO toItemResponseDto(GiftCertificateItems item) {
        if (item.getGiftCertificates().getType() == VALUE) {
            return CreateGiftCertificateRequestDTO.GiftCertificateItemDTO.builder()
                    .value(item.getValue())
                    .build();
        } else {
            TicketTypes ticketType = ticketTypesRepository.findById(item.getTicketTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found: " + item.getTicketTypeId()));

            return CreateGiftCertificateRequestDTO.GiftCertificateItemDTO.builder()
                    .ticketTypeId(ticketType.getRefNo())
                    .ticketTypeName(ticketType.getName())
                    .quantity(item.getQuantity())
                    .build();
        }
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
}