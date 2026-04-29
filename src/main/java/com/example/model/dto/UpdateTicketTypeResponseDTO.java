package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateTicketTypeResponseDTO {
    private String id;

    private String eventId;

    private String name;

    private List<TicketPricePeriodDTO> periods;

    private Integer capacity;

    private String description;

    private Enums.TicketTypeStatus status;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    private String message;

    private LocalDateTime timestamp;
}
