package com.example.model.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateBookingRequestDTO {
    @Schema(description = "List of attendees", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<CreateBookingRequestDTO.AttendeeDTO> attendees;

    private String notes;
}