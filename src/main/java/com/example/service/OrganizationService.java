package com.example.service;

import com.example.constant.Enums;
import com.example.exception.organization.OrganizationNotFoundException;
import com.example.mapper.OrganizationMapper;
import com.example.model.dto.*;
import com.example.model.entity.Organizations;
import com.example.repository.OrganizationsRepository;
import com.example.utils.ReferenceNoGenerator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationsRepository organizationsRepository;
    private final OrganizationMapper mapper;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final AuditService auditService;

    @Transactional
    public CreateOrganizationResponseDTO createOrganization(CreateOrganizationRequestDTO dto) {
        Organizations organization = mapper.toEntity(dto);
        organization.setRefNo(referenceNoGenerator.generateOrganizationReference());
        organization.setStatus(Enums.OrganizationStatus.ACTIVE);
        Organizations saved = organizationsRepository.save(organization);
        CreateOrganizationResponseDTO createOrganizationResponseDTO = mapper.toCreateResponseDTO(saved);
        createOrganizationResponseDTO.setMessage("Organization created successfully");
        createOrganizationResponseDTO.setTimestamp(ZonedDateTime.now());

        auditService.record("CREATE_ORGANIZATION",
                Organizations.class.getName(),
                organization.getId(),
                null,
                "Create organization successfully"
        );
        return createOrganizationResponseDTO;
    }

    @Transactional
    public UpdateOrganizationResponseDTO updateOrganization(String orgRefNo, CreateOrganizationRequestDTO dto) {
        Organizations organization = organizationsRepository.findByRefNo(orgRefNo)
                .orElseThrow(() -> new OrganizationNotFoundException(String.format("Organization %s not found)", orgRefNo)));

        if (dto.getName() != null) organization.setName(dto.getName());
        if (dto.getIndustry() != null) organization.setIndustry(dto.getIndustry());
        if (dto.getCompanyType() != null) organization.setCompanyType(dto.getCompanyType());
        if (dto.getCompanyGroup() != null) organization.setCompanyGroup(dto.getCompanyGroup());

        Organizations updated = organizationsRepository.save(organization);
        UpdateOrganizationResponseDTO updateOrganizationResponseDTO = mapper.toUpdateResponseDTO(updated);
        updateOrganizationResponseDTO.setMessage("Organization updated successfully");
        updateOrganizationResponseDTO.setTimestamp(ZonedDateTime.now());

        auditService.record("UPDATE_ORGANIZATION",
                Organizations.class.getName(),
                organization.getId(),
                null,
                "Update organization successfully"
        );
        return updateOrganizationResponseDTO;
    }

    @Transactional
    public DeleteOrganizationResponseDTO deleteOrganization(String orgRefNo) {
        if (!organizationsRepository.existsByRefNo(orgRefNo)) {
            throw new OrganizationNotFoundException(String.format("Organization %s not found)", orgRefNo));
        }

        ZonedDateTime deletedAt = ZonedDateTime.now();
        organizationsRepository.updateStatusByOrganizationRefNo(orgRefNo, Enums.OrganizationStatus.INACTIVE, deletedAt);
        DeleteOrganizationResponseDTO deleteOrganizationResponseDTO = new DeleteOrganizationResponseDTO();
        deleteOrganizationResponseDTO.setMessage("Organization deleted successfully");
        deleteOrganizationResponseDTO.setTimestamp(deletedAt);

        auditService.record("DELETE_ORGANIZATION",
                Organizations.class.getName(),
                null,
                null,
                "Delete organization successfully"
        );
        return deleteOrganizationResponseDTO;
    }

    public CreateOrganizationResponseDTO getOrganization(String orgRefNo) {
        Organizations organization = organizationsRepository.findByRefNo(orgRefNo)
                .orElseThrow(() -> new OrganizationNotFoundException(String.format("Organization %s not found)", orgRefNo)));
        return mapper.toCreateResponseDTO(organization);
    }

    public GetListOrganizationResponseDTO getAllOrganizations(Pageable pageable, String search) {
        Page<Organizations> organizations;

        if (StringUtils.isNotBlank(search)) {
            organizations = organizationsRepository.findBySearchTerm(search.trim(), pageable);
        } else {
            organizations = organizationsRepository.findAllActive(pageable);
        }

        return mapper.toGetListResponse(organizations);
    }
}
