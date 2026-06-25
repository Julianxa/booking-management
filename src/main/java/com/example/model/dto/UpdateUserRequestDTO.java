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
public class UpdateUserRequestDTO extends AbstractPartialUpdateDto {
    @JsonProperty("email")
    private String email;
    @JsonProperty("gender")
    private Character gender;
    @JsonProperty("country")
    private String country;
    @JsonProperty("phone")
    private String phone;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("org_id")
    private String orgId;
}
