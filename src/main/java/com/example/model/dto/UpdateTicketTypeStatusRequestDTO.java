package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateTicketTypeStatusRequestDTO {
    @Schema(
            description = "Ticket Type status",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("status")
    private Enums.TicketTypeStatus status;
}
