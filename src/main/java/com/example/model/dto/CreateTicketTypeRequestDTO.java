package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
    @JsonProperty("name")
    private String name;

    @JsonProperty("name_zh_cn")
    private String nameZhCn;

    @JsonProperty("name_zh_hk")
    private String nameZhHk;

    @JsonProperty("description")
    @Size(max = 255, message = "Description ≤ 255 characters")
    private String description;

    @JsonProperty("description_zh_cn")
    @Size(max = 255, message = "Description ≤ 255 characters")
    private String descriptionZhCn;

    @JsonProperty("description_zh_hk")
    @Size(max = 255, message = "Description ≤ 255 characters")
    private String descriptionZnHk;

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
