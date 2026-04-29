package com.example.mapper;

import com.example.model.dto.CreateOrganizationRequestDTO;
import com.example.model.dto.CreateOrganizationResponseDTO;
import com.example.model.dto.GetListOrganizationResponseDTO;
import com.example.model.dto.UpdateOrganizationResponseDTO;
import com.example.model.entity.Organizations;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Organizations toEntity(CreateOrganizationRequestDTO dto);

    @Mapping(target = "id", source = "refNo")
    CreateOrganizationResponseDTO toCreateResponseDto(Organizations entity);

    @Mapping(target = "id", source = "refNo")
    UpdateOrganizationResponseDTO toUpdateResponseDto(Organizations entity);

    default GetListOrganizationResponseDTO toGetListResponse(Page<Organizations> page) {
        GetListOrganizationResponseDTO response = new GetListOrganizationResponseDTO();

        response.setContent(
                page.getContent().stream()
                        .map(this::toCreateResponseDto)
                        .toList()
        );

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

    private GetListOrganizationResponseDTO.PageableDetail createPageableDetail(Page<?> page) {
        GetListOrganizationResponseDTO.PageableDetail pd = new GetListOrganizationResponseDTO.PageableDetail();
        pd.setPageNumber(page.getNumber());
        pd.setPageSize(page.getSize());
        pd.setOffset(page.getPageable().getOffset());
        pd.setPaged(page.getPageable().isPaged());
        pd.setUnpaged(page.getPageable().isUnpaged());
        pd.setSort(createSortDetail(page.getSort()));
        return pd;
    }

    private GetListOrganizationResponseDTO.SortDetail createSortDetail(Sort sort) {
        GetListOrganizationResponseDTO.SortDetail sd = new GetListOrganizationResponseDTO.SortDetail();
        sd.setEmpty(sort.isEmpty());
        sd.setSorted(sort.isSorted());
        sd.setUnsorted(sort.isUnsorted());
        return sd;
    }
}