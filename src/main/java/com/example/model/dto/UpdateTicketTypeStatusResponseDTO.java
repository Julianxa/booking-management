package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateTicketTypeStatusResponseDTO {
    @JsonProperty("id")
    private String id;
    @JsonProperty("ticket_type_id")
    private String ticketTypeId;
    @JsonProperty("status")
    private Enums.TicketTypeStatus status;
    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
