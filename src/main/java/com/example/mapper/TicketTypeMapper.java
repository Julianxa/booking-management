package com.example.mapper;

import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.dto.CreateTicketTypeRequestDTO;
import com.example.model.dto.CreateTicketTypeResponseDTO;
import com.example.model.dto.UpdateTicketTypeResponseDTO;
import com.example.model.entity.TicketTypes;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TicketTypeMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    TicketTypes toEntity(CreateTicketTypeRequestDTO dto);

    @Mapping(source = "refNo", target = "id")
    @Mapping(source = "event.refNo", target = "eventId")
    CreateTicketTypeResponseDTO toCreateResponseDto(TicketTypes entity);

    @Mapping(source = "refNo", target = "id")
    @Mapping(source = "event.refNo", target = "eventId")
    UpdateTicketTypeResponseDTO toUpdateResponseDto(TicketTypes entity);

    static CreateBookingRequestDTO.TicketTypeDTO toTicketTypeDTO(TicketTypes ticketTypes) {
        if (ticketTypes == null) {
            return null;
        }

        CreateBookingRequestDTO.TicketTypeDTO dto = new CreateBookingRequestDTO.TicketTypeDTO();

        dto.setId(ticketTypes.getRefNo());
        dto.setName(ticketTypes.getName());
        dto.setDescription(ticketTypes.getDescription());
        dto.setStatus(ticketTypes.getStatus());
        // dto.setCapacity(ticketType.getCapacity());

        return dto;
    }
}
