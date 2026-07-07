package com.example.mapper;

import com.example.constant.Enums;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.dto.CreateTicketTypeRequestDTO;
import com.example.model.dto.CreateTicketTypeResponseDTO;
import com.example.model.dto.UpdateTicketTypeResponseDTO;
import com.example.model.entity.TicketTypes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

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
    CreateTicketTypeResponseDTO toCreateResponseDTO(TicketTypes entity);

    @Mapping(source = "refNo", target = "id")
    @Mapping(source = "event.refNo", target = "eventId")
    UpdateTicketTypeResponseDTO toUpdateResponseDTO(TicketTypes entity);

    static CreateBookingRequestDTO.TicketTypeDTO toTicketTypeDTO(TicketTypes ticketTypes) {
        if (ticketTypes == null) {
            return null;
        }

        CreateBookingRequestDTO.TicketTypeDTO dto = new CreateBookingRequestDTO.TicketTypeDTO();

        dto.setId(ticketTypes.getRefNo());
        dto.setName(ticketTypes.getName());
        dto.setNameZhCn(ticketTypes.getNameZhCn());
        dto.setNameZhHk(ticketTypes.getNameZhHk());
        dto.setDescription(ticketTypes.getDescription());
        dto.setDescriptionZhCn(ticketTypes.getDescriptionZhCn());
        dto.setDescriptionZhHk(ticketTypes.getDescriptionZhHk());
        dto.setStatus(ticketTypes.getStatus());

        return dto;
    }

    static void copyLocalizedFields(
            TicketTypes source, CreateBookingRequestDTO.TicketTypeDTO target) {
        if (source == null || target == null) {
            return;
        }
        target.setName(source.getName());
        target.setNameZhCn(source.getNameZhCn());
        target.setNameZhHk(source.getNameZhHk());
        target.setDescription(source.getDescription());
        target.setDescriptionZhCn(source.getDescriptionZhCn());
        target.setDescriptionZhHk(source.getDescriptionZhHk());
        target.setStatus(source.getStatus());
    }

    static String resolveName(
            CreateBookingRequestDTO.TicketTypeDTO ticketType, Enums.Language language) {
        if (ticketType == null) {
            return "";
        }
        return switch (language != null ? language : Enums.Language.EN) {
            case CN -> firstNonBlank(ticketType.getNameZhCn(), ticketType.getName());
            case HK -> firstNonBlank(ticketType.getNameZhHk(), ticketType.getName());
            default -> nullToBlank(ticketType.getName());
        };
    }

    static String resolveDescription(
            CreateBookingRequestDTO.TicketTypeDTO ticketType, Enums.Language language) {
        if (ticketType == null) {
            return "";
        }
        return switch (language != null ? language : Enums.Language.EN) {
            case CN -> firstNonBlank(ticketType.getDescriptionZhCn(), ticketType.getDescription());
            case HK -> firstNonBlank(ticketType.getDescriptionZhHk(), ticketType.getDescription());
            default -> nullToBlank(ticketType.getDescription());
        };
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback == null ? "" : fallback;
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
