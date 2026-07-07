package com.example.converter;

import com.example.exception.ticket.TicketTypeNotFoundException;
import com.example.mapper.TicketTypeMapper;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.BookingItems;
import com.example.model.entity.TicketTypes;
import com.example.repository.TicketTypesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingItemsConverter {
    private final TicketTypesRepository ticketTypesRepository;

    public List<CreateBookingRequestDTO.TicketTypeDTO> toTicketTypeDTOs(List<BookingItems> items) {
        return items.stream()
                .map(item -> {
                    TicketTypes ticketType = ticketTypesRepository.findById(item.getTicketTypeId())
                            .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", item.getTicketTypeId())));

                    CreateBookingRequestDTO.TicketTypeDTO dto = TicketTypeMapper.toTicketTypeDTO(ticketType);
                    dto.setQuantity(item.getQuantity());
                    return dto;
                })
                .toList();
    }
}
