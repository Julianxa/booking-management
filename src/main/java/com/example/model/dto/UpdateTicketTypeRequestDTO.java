package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
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
public class UpdateTicketTypeRequestDTO {
    @Size(min = 1, max = 100, message = "Name must be 1–100 characters")
    @JsonProperty("name")
    private String name;

    @Size(min = 1, max = 100, message = "Name must be 1–100 characters")
    @JsonProperty("name_zh_cn")
    private String nameZhCn;

    @Size(min = 1, max = 100, message = "Name must be 1–100 characters")
    @JsonProperty("name_zh_hk")
    private String nameZhHk;

    @JsonProperty("periods")
    private List<TicketPricePeriodDTO> periods = List.of();

    public boolean hasPeriods() {
        return periods != null && !periods.isEmpty();
    }

    @Min(value = 0, message = "Capacity must be non-negative")
    private Integer capacity;

    @Size(max = 255, message = "Description ≤ 255 characters")
    private String description;

    @Size(max = 255, message = "Description ≤ 255 characters")
    @JsonProperty("description_zh_cn")
    private String descriptionZhCn;

    @Size(max = 255, message = "Description ≤ 255 characters")
    @JsonProperty("description_zh_hk")
    private String descriptionZhHk;
}
