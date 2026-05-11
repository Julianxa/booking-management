package com.example.converter;

import com.example.exception.ResourceNotFoundException;
import com.example.model.dto.CreateGiftCertificateRequestDTO;
import com.example.model.entity.GiftCertificateItems;
import com.example.model.entity.GiftCertificates;
import com.example.model.entity.TicketTypes;
import com.example.repository.TicketTypesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.constant.Enums.GiftCertificateType.VALUE;

@Component
@RequiredArgsConstructor
public class GiftCertificateItemsConverter {
    private final TicketTypesRepository ticketTypesRepository;

    public CreateGiftCertificateRequestDTO.GiftCertificateItemDTO toGiftCertificateItemDTO(GiftCertificateItems item) {
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

    public List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> toGiftCertificateItemDTOs(GiftCertificates gc) {
        return gc.getItems().stream()
                .map(this::toGiftCertificateItemDTO)
                .collect(Collectors.toList());
    }
}
