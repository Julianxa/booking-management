package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateGiftCertificateResponseDTO {
    @JsonProperty("promo_code")
    private String promoCode;
    @JsonProperty("type")
    private Enums.GiftCertificateType type;
    @JsonProperty("expiry_date")
    private LocalDate expiryDate;
    @JsonProperty("event_id")
    private String eventId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("quantity")
    private Integer quantity;
    @JsonProperty("remaining_quantity")
    private Integer remainingQuantity;
    @JsonProperty("message_to_recipient")
    private String messageToRecipient;
    @JsonProperty("status")
    private Enums.GiftCertificateStatus status;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    @JsonProperty("items")
    private List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> items;
}