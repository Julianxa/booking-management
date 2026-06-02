package com.example.converter;

import com.example.exception.event.EventNotFoundException;
import com.example.mapper.EventMapper;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.dto.CreateBookingResponseDTO;
import com.example.model.entity.*;
import com.example.repository.*;
import com.example.utils.QRCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingsConverter {
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingAttendeesRepository bookingAttendeesRepository;
    private final EventsRepository eventsRepository;
    private final UsersRepository usersRepository;
    private final BookingEventsConverter bookingEventsConverter;
    private final BookingItemsConverter bookingItemsConverter;
    private final EventMapper eventMapper;
    private final QRCodeGenerator qRCodeGenerator;

    public CreateBookingResponseDTO toCreateBookingResponseDTO(Bookings booking, String eventRefNo, String preFetchedPromoCode) {
        List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs = toBookingEventDTOs(booking, eventRefNo);

        return CreateBookingResponseDTO.builder()
                .id(booking.getRefNo())
                .type(booking.getType())
                .totalPaidAmount(booking.getTotalPaidPrice())
                .discount(booking.getDiscount())
                .finalPaidAmount(booking.getFinalPaidAmount())
                .currency(booking.getCurrency())
                .promoCode(preFetchedPromoCode)
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .bookingEvents(bookingEventDTOs)
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

        String checkInToken = bookingEvent.getVerificationToken();
        String qrCodeBase64 = qRCodeGenerator.generateQrCodeBase64(checkInToken);

        CreateBookingRequestDTO.EventDTO eventDTO;
        if (eventRefNo != null) {
            Events event = eventsRepository.findByRefNo(eventRefNo)
                    .orElseThrow(() -> new EventNotFoundException(String.format("Event %s not found", eventRefNo)));
            eventDTO = eventMapper.toEventDTO(event, bookingEvent);
        } else {
            Events event = eventsRepository.findById(bookingEvent.getEvent().getId())
                    .orElseThrow(() -> new EventNotFoundException("Event not found"));
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
                .answer(bookingEvent.getAnswer())
                .attendees(attendeeDTOs)
                .tickets(ticketDTOs)
                .qrCodeBase64(qrCodeBase64)
                .build();
    }
}
