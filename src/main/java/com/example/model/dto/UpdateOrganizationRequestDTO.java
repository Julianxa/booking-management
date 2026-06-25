package com.example.model.dto;

import com.example.jackson.AbstractPartialUpdateDto;
import com.example.jackson.PartialUpdate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@PartialUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateOrganizationRequestDTO extends AbstractPartialUpdateDto {
    @JsonProperty("name")
    private String name;
    @JsonProperty("industry")
    private String industry;
    @JsonProperty("company_type")
    private String companyType;
    @JsonProperty("company_group")
    private String companyGroup;
}
