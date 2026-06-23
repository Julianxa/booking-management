package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateGiftCertificateStatusResponseDTO {
    @JsonProperty("id")
    private String id;
    @JsonProperty("promo_code")
    private String promoCode;
    @JsonProperty("status")
    private Enums.GiftCertificateStatus status;
    @JsonProperty("cancelled_at")
    private ZonedDateTime cancelledAt;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;
}
