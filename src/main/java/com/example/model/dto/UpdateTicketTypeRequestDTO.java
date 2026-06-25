package com.example.model.dto;

import com.example.jackson.AbstractPartialUpdateDto;
import com.example.jackson.PartialUpdate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@PartialUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateTicketTypeRequestDTO extends AbstractPartialUpdateDto {
    @JsonProperty("name")
    private String name;

    @JsonProperty("name_zh_cn")
    private String nameZhCn;

    @JsonProperty("name_zh_hk")
    private String nameZhHk;

    @JsonProperty("periods")
    private List<TicketPricePeriodDTO> periods;

    @JsonProperty("capacity")
    private Integer capacity;

    @JsonProperty("description")
    private String description;

    @JsonProperty("description_zh_cn")
    private String descriptionZhCn;

    @JsonProperty("description_zh_hk")
    private String descriptionZhHk;
}
