package com.example.mapper;

import com.example.model.dto.*;
import com.example.model.entity.EmailTemplates;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmailTemplateMapper {
    @Mapping(target = "id", source = "template.refNo")
    @Mapping(target = "templateName", source = "template.templateName")
    @Mapping(target = "subject", source = "template.subject")
    @Mapping(target = "mainBody", source = "template.mainBody")
    @Mapping(target = "importantInfoIntro", source = "template.importantInfoIntro")
    @Mapping(target = "importantInfoBody", source = "template.importantInfoBody")
    @Mapping(target = "contactBody", source = "template.contactBody")
    @Mapping(target = "createdAt", source = "template.createdAt")
    @Mapping(target = "updatedAt", source = "template.updatedAt")
    UpdateEmailTemplatesResponseDTO toUpdateResponseDto(EmailTemplates template);

    @Mapping(target = "id", source = "template.refNo")
    @Mapping(target = "templateName", source = "template.templateName")
    @Mapping(target = "subject", source = "template.subject")
    @Mapping(target = "mainBody", source = "template.mainBody")
    @Mapping(target = "importantInfoIntro", source = "template.importantInfoIntro")
    @Mapping(target = "importantInfoBody", source = "template.importantInfoBody")
    @Mapping(target = "contactBody", source = "template.contactBody")
    @Mapping(target = "createdAt", source = "template.createdAt")
    @Mapping(target = "updatedAt", source = "template.updatedAt")
    GetEmailTemplateResponseDTO toResponseDto(EmailTemplates template);

    default GetListEmailTemplatesResponseDTO toGetListResponse(
            Page<EmailTemplates> page,
            List<GetEmailTemplateResponseDTO> content) {

        GetListEmailTemplatesResponseDTO.PageableDetail pageableDetail = new GetListEmailTemplatesResponseDTO.PageableDetail();
        pageableDetail.setOffset(page.getPageable().getOffset());
        pageableDetail.setPageNumber(page.getPageable().getPageNumber());
        pageableDetail.setPageSize(page.getPageable().getPageSize());
        pageableDetail.setPaged(true);
        pageableDetail.setUnpaged(false);

        GetListEmailTemplatesResponseDTO.SortDetail sortDetail = new GetListEmailTemplatesResponseDTO.SortDetail();
        sortDetail.setEmpty(page.getSort().isEmpty());
        sortDetail.setSorted(page.getSort().isSorted());
        sortDetail.setUnsorted(page.getSort().isUnsorted());
        pageableDetail.setSort(sortDetail);

        return GetListEmailTemplatesResponseDTO.builder()
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
