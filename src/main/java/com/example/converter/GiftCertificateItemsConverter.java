package com.example.converter;

import com.example.constant.Enums;
import com.example.exception.ticket.TicketTypeNotFoundException;
import com.example.model.dto.CreateGiftCertificateRequestDTO;
import com.example.model.entity.GiftCertificateItems;
import com.example.model.entity.GiftCertificates;
import com.example.model.entity.TicketTypes;
import com.example.repository.TicketTypesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.constant.Enums.GiftCertificateType.PERCENT;
import static com.example.constant.Enums.GiftCertificateType.PERSONAL_PERCENT;
import static com.example.constant.Enums.GiftCertificateType.PERSONAL_VALUE;
import static com.example.constant.Enums.GiftCertificateType.VALUE;

@Component
@RequiredArgsConstructor
public class GiftCertificateItemsConverter {
    private final TicketTypesRepository ticketTypesRepository;

    public CreateGiftCertificateRequestDTO.GiftCertificateItemDTO toGiftCertificateItemDTO(GiftCertificateItems item) {
        Enums.GiftCertificateType type = item.getGiftCertificates().getType();
        if (type == VALUE || type == PERSONAL_VALUE || type == PERCENT || type == PERSONAL_PERCENT) {
            return CreateGiftCertificateRequestDTO.GiftCertificateItemDTO.builder()
                    .value(item.getValue())
                    .build();
        } else {
            TicketTypes ticketType = ticketTypesRepository.findById(item.getTicketTypeId())
                    .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", item.getTicketTypeId())));

            return CreateGiftCertificateRequestDTO.GiftCertificateItemDTO.builder()
                    .ticketTypeId(ticketType.getRefNo())
                    .ticketTypeName(ticketType.getName())
                    .quantity(item.getQuantity())
                    .build();
        }
    }

    public List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> toGiftCertificateItemDTOs(GiftCertificates gc) {
        return gc.getItems().stream()
                .map(this::toGiftCertificateItemDTO)
                .collect(Collectors.toList());
    }
}
