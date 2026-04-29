package com.example.mapper;

import com.example.model.dto.*;
import com.example.model.entity.GiftCertificates;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GiftCertificateMapper {
    default CreateGiftCertificateResponseDTO toResponseDto(String userRefNo, String eventId, GiftCertificates giftCertificates, List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> giftCertificateItemDtos) {
        return CreateGiftCertificateResponseDTO.builder()
                .promoCode(giftCertificates.getPromoCode())
                .type(giftCertificates.getType())
                .expiryDate(giftCertificates.getExpiryDate())
                .eventId(eventId)
                .userId(userRefNo)
                .quantity(giftCertificates.getQuantity())
                .remainingQuantity(giftCertificates.getRemainingQuantity())
                .messageToRecipient(giftCertificates.getMessageToRecipient())
                .createdAt(giftCertificates.getCreatedAt())
                .updatedAt(giftCertificates.getUpdatedAt())
                .items(giftCertificateItemDtos)
                .build();
    }

    default GetListGiftCertificateResponseDTO toGetListResponse(
            Page<GiftCertificates> page,
            List<CreateGiftCertificateResponseDTO> content) {

        GetListGiftCertificateResponseDTO.PageableDetail pageableDetail = new GetListGiftCertificateResponseDTO.PageableDetail();
        pageableDetail.setOffset(page.getPageable().getOffset());
        pageableDetail.setPageNumber(page.getPageable().getPageNumber());
        pageableDetail.setPageSize(page.getPageable().getPageSize());
        pageableDetail.setPaged(true);
        pageableDetail.setUnpaged(false);

        GetListGiftCertificateResponseDTO.SortDetail sortDetail = new GetListGiftCertificateResponseDTO.SortDetail();
        sortDetail.setEmpty(page.getSort().isEmpty());
        sortDetail.setSorted(page.getSort().isSorted());
        sortDetail.setUnsorted(page.getSort().isUnsorted());
        pageableDetail.setSort(sortDetail);

        return GetListGiftCertificateResponseDTO.builder()
                .content(content)
                .pageable(pageableDetail)
                .last(page.isLast())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .size(page.getSize())
                .number(page.getNumber())
                .first(page.isFirst())
                .numberOfElements(page.getNumberOfElements())
                .empty(page.isEmpty())
                .build();
    }
}
