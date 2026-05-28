package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrganizationResponseDTO {
    @JsonProperty("id")
    private String id; // refNo
    @JsonProperty("name")
    private String name;
    @JsonProperty("industry")
    private String industry;
    @JsonProperty("company_type")
    private String companyType;
    @JsonProperty("company_group")
    private String companyGroup;
    @JsonProperty("status")
    private Enums.OrganizationStatus status;
    @JsonProperty("created_at")
    private ZonedDateTime createdAt;
    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;
    @JsonProperty("deleted_at")
    private ZonedDateTime deletedAt;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;
}
