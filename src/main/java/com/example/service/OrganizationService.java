package com.example.service;

import com.example.constant.Enums;
import com.example.exception.ResourceNotFoundException;
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

import java.sql.SQLException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationsRepository organizationsRepository;
    private final OrganizationMapper mapper;
    private final ReferenceNoGenerator referenceNoGenerator;

    @Transactional
    public CreateOrganizationResponseDTO createOrganization(CreateOrganizationRequestDTO dto) throws SQLException {
        Organizations organization = mapper.toEntity(dto);
        organization.setRefNo(referenceNoGenerator.generateOrganizationReference());
        organization.setStatus(Enums.OrganizationStatus.ACTIVE);
        Organizations saved = organizationsRepository.save(organization);
        CreateOrganizationResponseDTO createOrganizationResponseDTO = mapper.toCreateResponseDto(saved);
        createOrganizationResponseDTO.setMessage("Organization created successfully");
        createOrganizationResponseDTO.setTimestamp(LocalDateTime.now());
        return createOrganizationResponseDTO;
    }

    @Transactional
    public UpdateOrganizationResponseDTO updateOrganization(String orgRefNo, CreateOrganizationRequestDTO dto) {
        Long orgId = organizationsRepository.findIdByRefNo(orgRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with reference no: " + orgRefNo));
        Organizations organization = organizationsRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));

        if (dto.getName() != null) organization.setName(dto.getName());
        if (dto.getIndustry() != null) organization.setIndustry(dto.getIndustry());
        if (dto.getCompanyType() != null) organization.setCompanyType(dto.getCompanyType());
        if (dto.getCompanyGroup() != null) organization.setCompanyGroup(dto.getCompanyGroup());

        Organizations updated = organizationsRepository.save(organization);
        UpdateOrganizationResponseDTO updateOrganizationResponseDTO = mapper.toUpdateResponseDto(updated);
        updateOrganizationResponseDTO.setMessage("Organization updated successfully");
        updateOrganizationResponseDTO.setTimestamp(LocalDateTime.now());
        return updateOrganizationResponseDTO;
    }

    @Transactional
    public DeleteOrganizationResponseDTO deleteOrganization(String orgRefNo) {
        if (!organizationsRepository.existsByRefNo(orgRefNo)) {
            throw new ResourceNotFoundException("Organization not found with code: " + orgRefNo);
        }

        LocalDateTime deletedAt = LocalDateTime.now();
        organizationsRepository.updateStatusByOrganizationRefNo(orgRefNo, Enums.OrganizationStatus.INACTIVE, deletedAt);
        DeleteOrganizationResponseDTO deleteOrganizationResponseDTO = new DeleteOrganizationResponseDTO();
        deleteOrganizationResponseDTO.setMessage("Organization deleted successfully");
        deleteOrganizationResponseDTO.setTimestamp(deletedAt);
        return deleteOrganizationResponseDTO;
    }

    public CreateOrganizationResponseDTO getOrganization(String orgRefNo) {
        Organizations organization = organizationsRepository.findByRefNo(orgRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with code: " + orgRefNo));
        return mapper.toCreateResponseDto(organization);
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
