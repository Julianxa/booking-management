package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResendBookingEmailRequestDTO {
    @JsonProperty("email_template_id")
    private String emailTemplateId;

    @NotNull(message = "email_type is required")
    @JsonProperty("email_type")
    private Enums.BookingEmailType emailType;
}
