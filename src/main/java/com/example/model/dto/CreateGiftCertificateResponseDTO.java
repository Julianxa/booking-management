package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
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
    @JsonProperty("effective_date")
    private LocalDate effectiveDate;
    @JsonProperty("expiry_date")
    private LocalDate expiryDate;
    @JsonProperty("event_id")
    private String eventId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("assignee_user_id")
    private String assigneeUserId;
    @JsonProperty("quantity")
    private Integer quantity;
    @JsonProperty("remaining_quantity")
    private Integer remainingQuantity;
    @JsonProperty("message_to_recipient")
    private String messageToRecipient;
    @JsonProperty("status")
    private Enums.GiftCertificateStatus status;
    @JsonProperty("created_at")
    private ZonedDateTime createdAt;
    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;
    @JsonProperty("items")
    private List<CreateGiftCertificateRequestDTO.GiftCertificateItemDTO> items;
}