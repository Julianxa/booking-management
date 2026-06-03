package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateTicketTypeResponseDTO {
    private String id;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("name_zh_cn")
    private String nameZhCn;

    @JsonProperty("name_zh_hk")
    private String nameZhHk;

    private List<TicketPricePeriodDTO> periods;

    private Integer capacity;

    @JsonProperty("description")
    private String description;

    @JsonProperty("description_zh_cn")
    private String descriptionZhCn;

    @JsonProperty("description_zh_hk")
    private String descriptionZhHk;

    private Enums.TicketTypeStatus status;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @JsonProperty("created_by")
    private Long createdBy;

    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;

    @JsonProperty("updated_by")
    private Long updatedBy;

    private String message;

    private ZonedDateTime timestamp;
}
