package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangePasswordRequestDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Old Password is required")
    @JsonProperty("old_password")
    private String oldPassword;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @JsonProperty("password")
    private String password;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Confirm Password is required")
    @JsonProperty("confirm_password")
    private String confirmPassword;
}
