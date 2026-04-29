package com.example.model.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitiateCheckinResponseDTO {
    @JsonProperty("booking_id")
    private String bookingId;

    @JsonProperty("booking_event")
    private CreateBookingRequestDTO.BookingEventDTO bookingEventDto;
}
