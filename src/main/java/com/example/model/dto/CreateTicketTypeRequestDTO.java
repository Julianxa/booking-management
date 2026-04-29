package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketTypeRequestDTO {
    @NotBlank(message = "Ticket type name is required")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    private String name;

    @JsonProperty("periods")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Periods is required")
    @Valid
    private List<TicketPricePeriodDTO> periods = List.of();

    public boolean hasPeriods() {
        return periods != null && !periods.isEmpty();
    }

//    @Min(value = 0, message = "Capacity must be non-negative")
//    @JsonProperty("capacity")
//    private Integer capacity;
}
