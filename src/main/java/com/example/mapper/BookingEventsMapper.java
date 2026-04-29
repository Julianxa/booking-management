package com.example.mapper;


import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.dto.InitiateCheckinResponseDTO;
import com.example.model.entity.BookingEvents;
import com.example.model.entity.Bookings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface BookingEventsMapper {
    @Mapping(target = "userId", source = "booking.userId")
    @Mapping(target = "attendees", source = "attendees")
    @Mapping(target = "createdAt", source = "booking.createdAt")
    @Mapping(target = "id", source = "bookingEvent.refNo")
    @Mapping(target = "status", source = "bookingEvent.status")
    @Mapping(target = "notes", source = "bookingEvent.notes")
    @Mapping(target = "qrCodeBase64", source = "qrCodeBase64")
    @Mapping(target = "tickets", source = "dto.tickets")
    @Mapping(target = "event", source = "dto.event")
    @Mapping(target = "total", source = "bookingEvent.total")
    CreateBookingRequestDTO.BookingEventDTO toCreateResponseDto(
            Bookings booking, BookingEvents bookingEvent, CreateBookingRequestDTO.BookingEventDTO dto,
            List<CreateBookingRequestDTO.AttendeeDTO> attendees, String qrCodeBase64);

    default InitiateCheckinResponseDTO toResponseDto(
            String userRefNo,
            BookingEvents bookingEvent,
            List<CreateBookingRequestDTO.TicketTypeDTO> ticketDTOs,
            List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs) {

        if (bookingEvent == null) {
            return null;
        }

        CreateBookingRequestDTO.BookingEventDTO eventDto = CreateBookingRequestDTO.BookingEventDTO.builder()
                .id(bookingEvent.getRefNo())
                .event(CreateBookingRequestDTO.EventDTO.builder()
                        .id(bookingEvent.getRefNo())
                        .eventDate(bookingEvent.getEventDate())
                        .eventTime(bookingEvent.getEventTime())
                        .build())
                .userId(userRefNo)
                .notes(bookingEvent.getNotes())
                .status(bookingEvent.getStatus())
                .attendees(attendeeDTOs)
                .tickets(ticketDTOs)
                .createdAt(bookingEvent.getBooking() != null ? bookingEvent.getBooking().getCreatedAt() : null)
                .build();

        return InitiateCheckinResponseDTO.builder()
                .bookingId(bookingEvent.getBooking() != null ? bookingEvent.getBooking().getRefNo() : null)
                .bookingEventDto(eventDto)
                .build();
    }

    @Mapping(target = "id", source = "eventRefNo")
    @Mapping(target = "eventDate", source = "be.eventDate")
    @Mapping(target = "eventTime", source = "be.eventTime")
    CreateBookingRequestDTO.EventDTO toEventDto(String eventRefNo, BookingEvents be);
}
