package com.example.mapper;

import com.example.model.dto.GetListRefundResponseDTO;
import com.example.model.dto.RefundResponseDTO;
import com.example.model.entity.Refunds;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RefundMapper {
    default RefundResponseDTO toCreateResponseDTO(String bookingRefNo, Refunds entity) {
        if (entity == null) {
            return null;
        }
        RefundResponseDTO dto = new RefundResponseDTO();
        dto.setId(entity.getRefNo());
        dto.setBookingId(bookingRefNo);
        dto.setRefundAmount(entity.getAmount());
        dto.setRefundCurrency(entity.getCurrency());
        dto.setRefundType(entity.getType());
        dto.setRemarks(entity.getRemarks());
        return dto;
    }

    default GetListRefundResponseDTO toGetListResponse(Page<Refunds> page,
                                                       List<RefundResponseDTO> content) {
        GetListRefundResponseDTO.PageableDetail pageableDetail = new GetListRefundResponseDTO.PageableDetail();
        pageableDetail.setOffset(page.getPageable().getOffset());
        pageableDetail.setPageNumber(page.getPageable().getPageNumber());
        pageableDetail.setPageSize(page.getPageable().getPageSize());
        pageableDetail.setPaged(true);
        pageableDetail.setUnpaged(false);

        GetListRefundResponseDTO.SortDetail sortDetail = new GetListRefundResponseDTO.SortDetail();
        sortDetail.setEmpty(page.getSort().isEmpty());
        sortDetail.setSorted(page.getSort().isSorted());
        sortDetail.setUnsorted(page.getSort().isUnsorted());
        pageableDetail.setSort(sortDetail);

        return GetListRefundResponseDTO.builder()
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
