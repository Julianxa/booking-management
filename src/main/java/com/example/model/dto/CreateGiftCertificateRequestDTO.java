package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
public class CreateGiftCertificateRequestDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of gift certificate")
    @NotBlank(message = "Gift Certificate PromoCode is required")
    @Valid
    @JsonProperty("promo_code")
    private String promoCode;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of gift certificate")
    @NotNull(message = "Gift Certificate Type is required")
    @Valid
    @JsonProperty("type")
    private Enums.GiftCertificateType type;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Expiry Date is required")
    @FutureOrPresent(message = "Expiry date cannot be in the past")
    @JsonProperty("expiry_date")
    private LocalDate expiryDate;

    @JsonProperty("event_id")
    private String eventId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    @JsonProperty("quantity")
    private Integer quantity = 1;

    @JsonProperty("message_to_recipient")
    private String messageToRecipient;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Items is required")
    @Valid
    @JsonProperty("items")
    private List<GiftCertificateItemDTO> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public static class GiftCertificateItemDTO {
        @JsonProperty("ticket_type_id")
        private String ticketTypeId;
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("ticket_type_name")
        private String ticketTypeName;
        @JsonProperty("quantity")
        private Integer quantity;
        @JsonProperty("value")
        private BigDecimal value;
        @JsonProperty("created_at")
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private LocalDateTime createdAt;
    }
}