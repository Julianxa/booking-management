package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateGiftCertificateResponseDTO {
    @JsonProperty("id")
    private String id;
    @JsonProperty("promo_code")
    private String promoCode;
    @FutureOrPresent(message = "Expiry date cannot be in the past")
    @JsonProperty("expiry_date")
    private LocalDate expiryDate;
    @JsonProperty("message_to_recipient")
    private String messageToRecipient;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
