package com.example.service;

import com.example.constant.Enums;
import com.example.converter.BookingItemsConverter;
import com.example.converter.GiftCertificateItemsConverter;
import com.example.exception.booking.BookingEventNotFoundException;
import com.example.exception.event.EventNotFoundException;
import com.example.exception.giftCertificate.*;
import com.example.exception.ticket.TicketPricePeriodNotFoundException;
import com.example.exception.ticket.TicketTypeNotFoundException;
import com.example.exception.user.UserNotFoundException;
import com.example.mapper.GiftCertificateMapper;
import com.example.model.dto.*;
import com.example.model.entity.*;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.*;
import com.example.utils.ReferenceNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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
    public CreateGiftCertificateResponseDTO createCertificate(String userSub, CreateGiftCertificateRequestDTO dto) {

        Users user = usersRepository.findByUserSub(userSub)
                .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", userSub)));

        Long assigneeUserId = null;
        if(dto.getType() == PERSONAL_EVENT || dto.getType() == PERSONAL_VALUE) {
            Users assigneeUser = usersRepository.findByRefNo(dto.getAssigneeUserId())
                    .orElseThrow(() -> new UserNotFoundException(String.format("Assignee user %s not found", dto.getAssigneeUserId())));
            assigneeUserId = assigneeUser.getId();

            if(dto.getQuantity() != 1) {
                throw new InvalidGCException("Quantity must be 1 for this type of gift certificate");
            }
        }

        Long eventId;
        if (dto.getType() == EVENT || dto.getType() == PERSONAL_EVENT) {
            // For EVENT type certificates, event ID is required
            eventId = eventsRepository.findIdByRefNo(dto.getEventId())
                    .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found for EVENT type certificate", dto.getEventId())));
        } else {
            // For VALUE type certificates, event ID is optional
            eventId = eventsRepository.findIdByRefNo(dto.getEventId()).orElse(null);
        }
        
        GiftCertificates gc = buildGiftCertificate(user.getId(), assigneeUserId, eventId, dto);

        if (giftCertificatesRepository.existsByPromoCode(gc.getPromoCode())) {
            throw new GCPromoCodeExistsException(String.format("Promotion code %s already exists", gc.getPromoCode()));
        }

        if (dto.getType() == EVENT || dto.getType() == PERSONAL_EVENT) {
            validateAndAddEventItems(gc, dto.getItems());
        } else {
            validateAndAddValueItems(gc, dto.getItems());
        }

        gc = giftCertificatesRepository.save(gc);

        auditService.record("CREATE_CERTIFICATE",
                GiftCertificates.class.getName(),
                gc.getId(),
                null,
                "Create gift certificate successfully"
        );

        return buildResponse(gc, user.getRefNo(), dto.getAssigneeUserId(), dto.getEventId());
    }

    @Transactional
    public UpdateGiftCertificateResponseDTO updateCertificate(String promoCode, UpdateGiftCertificateRequestDTO dto) {
        GiftCertificates giftCertificates = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new GCNotFoundException(String.format("Gift Certificate with promotion code %s not found", promoCode)));

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
        updateGiftCertificateResponseDTO.setTimestamp(ZonedDateTime.now());

        auditService.record("UPDATE_CERTIFICATE",
                GiftCertificates.class.getName(),
                giftCertificates.getId(),
                null,
                "Update gift certificate successfully"
        );
        return updateGiftCertificateResponseDTO;
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

        if (giftCertificate.getType() == VALUE) {
            GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(giftCertificate.getId())
                    .orElseThrow(() -> new GCNotFoundException(String.format("Value gift certificate %s not found", giftCertificate.getId())));

            return new GiftCertificateApplicationResult(giftCertificate, List.of(), item.getValue());
        } else if (giftCertificate.getType() == EVENT) {
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

    private GiftCertificates buildGiftCertificate(Long userId, Long assigneeUserId, Long eventId, CreateGiftCertificateRequestDTO dto) {
        return GiftCertificates.builder()
                .refNo(referenceNoGenerator.generateGiftCertificateReference())
                .promoCode(dto.getPromoCode())
                .eventId(eventId)
                .userId(userId)
                .assigneeUserId(assigneeUserId)
                .type(dto.getType())
                .effectiveDate(dto.getEffectiveDate())
                .expiryDate(dto.getExpiryDate())
                .quantity(dto.getQuantity())
                .remainingQuantity(dto.getQuantity())
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
                gc.getItems().add(GiftCertificateItems.builder()
                        .giftCertificates(gc)
                        .value(itemDTO.getValue())
                        .build());
            }
        }
    }

    public CreateGiftCertificateResponseDTO getCertificate(String promoCode) {
        GiftCertificates gc = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new GCNotFoundException(String.format("Gift certificate with promotion code %s not found", promoCode)));

        String userRefNo = usersRepository.findRefNoById(gc.getUserId()).orElse(null);
        String assigneeUserRefNo = usersRepository.findRefNoById(gc.getAssigneeUserId()).orElse(null);
        String eventRefNo = eventsRepository.findRefNoById(gc.getEventId()).orElse(null);

        CreateGiftCertificateResponseDTO response = giftCertificateMapper.toCreateResponseDTO(
                userRefNo, assigneeUserRefNo, eventRefNo, gc, giftCertificateItemsConverter.toGiftCertificateItemDTOs(gc));

        response.setStatus(findStatusByCertificate(gc));
        return response;
    }

    public GetListGiftCertificateResponseDTO getGiftCertificates(
            Pageable pageable, String eventRefNo) {
        Page<GiftCertificates> giftCertificatesPage;
        Long eventId = eventRefNo != null ? eventsRepository.findIdByRefNo(eventRefNo)
                .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)))
                : null;
        if (eventId != null) {
            giftCertificatesPage = giftCertificatesRepository.findByEventId(eventId, pageable);
        } else {
            giftCertificatesPage = giftCertificatesRepository.findAll(pageable);
        }

        List<CreateGiftCertificateResponseDTO> content = giftCertificatesPage.getContent().stream()
                .map(giftCertificate -> {
                    String userRefNo = usersRepository.findRefNoById(giftCertificate.getUserId())
                            .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", giftCertificate.getUserId())));
                    String assigneeUserRefNo = usersRepository.findRefNoById(giftCertificate.getAssigneeUserId())
                            .orElse(null);

                    String evtRefNo = eventsRepository.findRefNoById(giftCertificate.getEventId())
                            .orElse(null);

                    return buildResponse(giftCertificate, userRefNo, assigneeUserRefNo,evtRefNo);
                })
                .toList();

        GetListGiftCertificateResponseDTO getListGiftCertificateResponseDTO = giftCertificateMapper.toGetListResponse(giftCertificatesPage, content);
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
        if (gc.getAssigneeUserId() != null && !gc.getAssigneeUserId().equals(userId)) throw new InvalidGCException("This gift certificate is not assigned to the user");
        return gc;
    }

    @Transactional
    private GiftCertificateApplicationResult applyValueType(GiftCertificates gc) {
        GiftCertificateItems item = giftCertificateItemRepository.getValueCertByGiftCertificateId(gc.getId())
                .orElseThrow(() -> new GCItemNotFoundException(String.format("Value gift certificate item not found with %s", gc.getId())));

        return new GiftCertificateApplicationResult(gc, List.of(), item.getValue());
    }

    @Transactional
    private GiftCertificateApplicationResult applyEventType(GiftCertificates gc, List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
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

        BigDecimal discount = getGiftCertificateDiscount(redeemedTickets);

        return new GiftCertificateApplicationResult(gc, redeemedTickets, discount);
    }

    private CreateGiftCertificateResponseDTO buildResponse(GiftCertificates gc, String userRefNo, String assigneeUserRefNo, String eventRefNo) {
        CreateGiftCertificateResponseDTO response = giftCertificateMapper.toCreateResponseDTO(
                userRefNo, assigneeUserRefNo, eventRefNo, gc, giftCertificateItemsConverter.toGiftCertificateItemDTOs(gc));
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
                .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", ticket.getId())));

        BigDecimal price = ticketPricePeriodsRepository.findActivePrice(ticketTypeId, null)
                .orElseThrow(() -> new TicketPricePeriodNotFoundException(String.format("Price period not found with ticket type %s", ticketTypeId)))
                .getPrice();

        return price.multiply(BigDecimal.valueOf(ticket.getQuantity()));
    }

    public UpdateGiftCertificateStatusResponseDTO updateGiftCertificateStatus(String promoCode, UpdateGiftCertificateStatusRequestDTO dto) {
        GiftCertificates giftCertificates = giftCertificatesRepository.findByPromoCode(promoCode)
                .orElseThrow(() -> new GCNotFoundException(String.format("Gift Certificate not found with promotion code %s", promoCode)));

        ZonedDateTime actionAt = ZonedDateTime.now();
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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public GiftCertificateApplicationResult validateAndCalculateGiftCertificate(Users loggedInUser, List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs, String promoCode) {
        if (promoCode == null) {
            return new GiftCertificateApplicationResult(null, List.of(), BigDecimal.ZERO);
        }

        Long userId = loggedInUser != null ? loggedInUser.getId() : null;

        GiftCertificates gc = validateGiftCertificateForBooking(promoCode, userId);

        GiftCertificateApplicationResult giftCertificateApplicationResult;
        if (gc.getType() == VALUE) {
            giftCertificateApplicationResult = applyValueType(gc);
        } else {
            giftCertificateApplicationResult = applyEventType(gc, bookingEventDTOs);
        }

        return giftCertificateApplicationResult;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void preserveGiftCertificate(Users user, Bookings booking, GiftCertificateApplicationResult giftCertificateApplicationResult) {
        Long userId = user != null ? user.getId() : null;

        GiftCertificates gc = giftCertificateApplicationResult.certificate();
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

        GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new GCRedemptionNotFoundException("Gift certificate redemption not found"));
        redemption.setStatus(SUCCESS);
        redemption.setRedeemedAt(ZonedDateTime.now());
        giftCertificateRedemptionRepository.save(redemption);

        return getCertificateRedemptionResult(booking);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelCertificateRedemption(Bookings booking) {
        GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingId(booking.getId())
                .orElse(null);
        if(redemption != null) {
            redemption.setStatus(FAILED);
            giftCertificateRedemptionRepository.save(redemption);

            GiftCertificates gc = giftCertificatesRepository.findById(redemption.getGiftCertificateId())
                    .orElseThrow(() -> new GCNotFoundException("Gift certificate not found"));
            gc.setRemainingQuantity(gc.getRemainingQuantity() + 1);
            giftCertificatesRepository.save(gc);
        }
    }
}