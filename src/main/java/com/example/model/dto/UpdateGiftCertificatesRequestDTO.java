package com.example.model.dto;

import com.example.jackson.AbstractPartialUpdateDto;
import com.example.jackson.PartialUpdate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@PartialUpdate
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateGiftCertificatesRequestDTO extends AbstractPartialUpdateDto {
    @JsonProperty("effective_date")
    private LocalDate effectiveDate;
    @JsonProperty("expiry_date")
    private LocalDate expiryDate;
    @JsonProperty("message_to_recipient")
    private String messageToRecipient;
}
