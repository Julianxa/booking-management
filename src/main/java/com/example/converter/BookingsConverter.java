package com.example.converter;

import com.example.config.AppProperties;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.EventMapper;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.dto.CreateBookingResponseDTO;
import com.example.model.entity.*;
import com.example.repository.*;
import com.example.utils.QRCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingsConverter {
    private final BookingEventsRepository bookingEventsRepository;
    private final GiftCertificatesRepository giftCertificatesRepository;
    private final GiftCertificateRedemptionRepository giftCertificateRedemptionRepository;
    private final BookingAttendeesRepository bookingAttendeesRepository;
    private final EventsRepository eventsRepository;
    private final UsersRepository usersRepository;
    private final BookingEventsConverter bookingEventsConverter;
    private final BookingItemsConverter bookingItemsConverter;
    private final EventMapper eventMapper;
    private final QRCodeGenerator qRCodeGenerator;
    private final AppProperties appProperties;


    public CreateBookingResponseDTO toCreateBookingResponseDTO(Bookings booking, String eventRefNo) {
        List<CreateBookingRequestDTO.BookingEventDTO> events = toBookingEventDTOs(booking, eventRefNo);

        String giftCertificatePromoCode = null;
        if (booking.getDiscount() != null && booking.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            GiftCertificateRedemptions redemption = giftCertificateRedemptionRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Gift Certificate Redemption not found by booking refNo: " + booking.getRefNo()));
            giftCertificatePromoCode = giftCertificatesRepository.findPromoCodeById(redemption.getGiftCertificateId());
        }

        return CreateBookingResponseDTO.builder()
                .id(booking.getRefNo())
                .totalPaidAmount(booking.getTotalPaidPrice())
                .discount(booking.getDiscount())
                .finalPaidAmount(booking.getFinalPaidAmount())
                .currency(booking.getCurrency())
                .promoCode(giftCertificatePromoCode)
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .bookingEvents(events)
                .build();
    }

    public List<CreateBookingRequestDTO.BookingEventDTO> toBookingEventDTOs(Bookings booking, String eventRefNo) {
        List<BookingEvents> bookingEvents = bookingEventsRepository.findByBookingId(booking.getId());
        List<CreateBookingRequestDTO.AttendeeDTO> attendees = bookingAttendeesRepository.findAttendeesByBookingId(booking.getId());

        Map<Long, List<BookingItems>> itemsByEvent = bookingEventsConverter.toBookingItemsByEventMap(bookingEvents);

        return bookingEvents.stream()
                .map(bookingEvent -> buildBookingEventDTO(booking, eventRefNo, bookingEvent, attendees, itemsByEvent))
                .toList();
    }

    private CreateBookingRequestDTO.BookingEventDTO buildBookingEventDTO(
            Bookings booking,
            String eventRefNo,
            BookingEvents bookingEvent,
            List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs,
            Map<Long, List<BookingItems>> itemsByEvent) {

        List<BookingItems> bookingItems = itemsByEvent.getOrDefault(bookingEvent.getId(), List.of());

        List<CreateBookingRequestDTO.TicketTypeDTO> ticketDTOs = bookingItemsConverter.toTicketTypeDTOs(bookingItems);

        String checkInUrl = appProperties.getBaseUrl()
                + appProperties.getCheckin().getPath()
                + bookingEvent.getVerificationToken();
        String qrCodeBase64 = qRCodeGenerator.generateQrCodeBase64(checkInUrl);

        CreateBookingRequestDTO.EventDTO eventDTO;
        if (eventRefNo != null) {
            Events event = eventsRepository.findByRefNo(eventRefNo)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            eventDTO = eventMapper.toEventDTO(event, bookingEvent);
        } else {
            Events event = eventsRepository.findById(bookingEvent.getEvent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            eventDTO = eventMapper.toEventDTO(event, bookingEvent);
        }

        String userRefNo = usersRepository.findActiveRefNoById(booking.getUserId()).orElse(null);

        return CreateBookingRequestDTO.BookingEventDTO.builder()
                .id(bookingEvent.getRefNo())
                .userId(userRefNo)
                .event(eventDTO)
                .total(bookingEvent.getTotal())
                .status(bookingEvent.getStatus())
                .notes(bookingEvent.getNotes())
                .attendees(attendeeDTOs)
                .tickets(ticketDTOs)
                .qrCodeBase64(qrCodeBase64)
                .build();
    }
}
