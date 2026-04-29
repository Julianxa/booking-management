package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateCheckinRequestDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Token is required")
    @JsonProperty("token")
    private String token;
}
