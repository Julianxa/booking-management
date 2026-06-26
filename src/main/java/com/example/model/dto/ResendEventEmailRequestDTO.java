package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResendEventEmailRequestDTO {
    @NotBlank(message = "event_id is required")
    @JsonProperty("event_id")
    private String eventId;

    @NotNull(message = "event_date is required")
    @JsonProperty("event_date")
    private LocalDate eventDate;

    @NotBlank(message = "event_time is required")
    @JsonProperty("event_time")
    private String eventTime;

    @JsonProperty("email_template_id")
    private String emailTemplateId;
}
