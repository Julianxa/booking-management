package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateGiftCertificateRequestDTO {
    @FutureOrPresent(message = "Expiry date cannot be in the past")
    @JsonProperty("expiry_date")
    private LocalDate expiryDate;
    @JsonProperty("message_to_recipient")
    private String messageToRecipient;
}
