package com.example.mapper;

import com.example.model.dto.UpdateEmailTemplatesResponseDTO;
import com.example.model.entity.EmailTemplates;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
}
