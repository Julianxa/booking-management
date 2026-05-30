package com.example.model.dto;

import com.example.model.entity.EventTimeSlotExceptionsHistory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetStatusHistoryResponseDTO {
    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("history")
    List<EventTimeSlotExceptionsHistory> history;

    @JsonProperty("message")
    private String message;
}
