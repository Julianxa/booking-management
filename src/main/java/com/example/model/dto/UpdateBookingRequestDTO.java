package com.example.model.dto;

import com.example.jackson.AbstractPartialUpdateDto;
import com.example.jackson.PartialUpdate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@PartialUpdate
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateBookingRequestDTO extends AbstractPartialUpdateDto {
    @Schema(
            description = "List of attendees. Omit to leave unchanged; send null to clear; send [] to remove all.",
            nullable = true
    )
    @Valid
    private List<CreateBookingRequestDTO.AttendeeDTO> attendees;

    @Schema(
            description = "Booking notes. Omit to leave unchanged; send null to clear.",
            nullable = true,
            example = "VIP guest"
    )
    private String notes;
}
