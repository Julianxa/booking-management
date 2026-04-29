package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateEventStatusRequestDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Event Date is required")
    @JsonProperty("event_date")
    private LocalDate eventDate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Event Time is required")
    @JsonProperty("event_time")
    private String eventTime;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Status is required")
    @JsonProperty("status")
    private Enums.EventStatus status;
}
