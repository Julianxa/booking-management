package com.example.mapper;

import com.example.model.dto.GetListReportsResponseDTO;
import com.example.model.dto.ReportSummaryResponseDTO;
import com.example.model.entity.Reports;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Mapper(componentModel = "spring")
public interface ReportMapper {
    @Mapping(target = "id", source = "refNo")
    @Mapping(target = "reportStartDate", source = "startDate")
    @Mapping(target = "reportEndDate", source = "endDate")
    @Mapping(target = "downloadUrl", ignore = true)
    ReportSummaryResponseDTO toSummaryResponseDTO(Reports entity);

    default GetListReportsResponseDTO toGetListResponse(Page<ReportSummaryResponseDTO> page) {
        GetListReportsResponseDTO response = new GetListReportsResponseDTO();
        response.setContent(page.getContent());
        response.setPageable(createPageableDetail(page));
        response.setSort(createSortDetail(page.getSort()));
        response.setLast(page.isLast());
        response.setFirst(page.isFirst());
        response.setTotalPages(page.getTotalPages());
        response.setTotalElements(page.getTotalElements());
        response.setSize(page.getSize());
        response.setNumber(page.getNumber());
        response.setNumberOfElements(page.getNumberOfElements());
        response.setEmpty(page.isEmpty());
        return response;
    }

    private GetListReportsResponseDTO.PageableDetail createPageableDetail(Page<?> page) {
        GetListReportsResponseDTO.PageableDetail pageableDetail =
                new GetListReportsResponseDTO.PageableDetail();
        pageableDetail.setPageNumber(page.getNumber());
        pageableDetail.setPageSize(page.getSize());
        pageableDetail.setOffset(page.getPageable().getOffset());
        pageableDetail.setPaged(page.getPageable().isPaged());
        pageableDetail.setUnpaged(page.getPageable().isUnpaged());
        pageableDetail.setSort(createSortDetail(page.getSort()));
        return pageableDetail;
    }

    private GetListReportsResponseDTO.SortDetail createSortDetail(Sort sort) {
        GetListReportsResponseDTO.SortDetail sortDetail = new GetListReportsResponseDTO.SortDetail();
        sortDetail.setEmpty(sort.isEmpty());
        sortDetail.setSorted(sort.isSorted());
        sortDetail.setUnsorted(sort.isUnsorted());
        return sortDetail;
    }
}
