package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationRequestDTO {
    @JsonProperty("name")
    private String name;
    @JsonProperty("industry")
    private String industry;
    @JsonProperty("company_type")
    private String companyType;
    @JsonProperty("company_group")
    private String companyGroup;
}
