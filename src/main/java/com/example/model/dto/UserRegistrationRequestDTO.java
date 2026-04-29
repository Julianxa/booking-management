package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRegistrationRequestDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "First name is required")
    @JsonProperty("first_name")
    private String firstName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Last name is required")
    @JsonProperty("last_name")
    private String lastName;
    @Schema(
            description = "User role",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("role")
    private Enums.UserRole role;
    @JsonProperty("country")
    private String country;
    @JsonProperty("gender")
    private Character gender;
    @JsonProperty("phone")
    private String phone;
    @JsonProperty("org_id")
    private String orgId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @JsonProperty("email")
    private String email;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @JsonProperty("password")
    private String password;
}
