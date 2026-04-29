package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailableDayDTO {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Day of week",
            example = "MON",
            allowableValues = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"})
    @NotBlank(message = "Day is required")
    @JsonProperty("day")
    private String day;

    @NotEmpty(message = "At least one start time is required")
    @Valid
    @Schema(description = "Start times of event",
            example = "[\"09:00\", \"10:00\", \"14:30\"]")
    @JsonProperty("startTimes")
    private List<String> startTimes;
}