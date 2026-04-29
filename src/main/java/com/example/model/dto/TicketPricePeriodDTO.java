package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketPricePeriodDTO {
    @JsonProperty("price")
    private BigDecimal price;
    @JsonProperty("effective_from")
    private LocalDateTime effectiveFrom;
    @JsonProperty("effective_to")
    private LocalDateTime effectiveTo;
    @JsonProperty("reason")
    private String reason;
}