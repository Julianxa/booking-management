package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class GetUserResponseDTO {
    @JsonProperty("id")
    private String id;

    @JsonProperty("user_sub")
    private String userSub;

    @JsonProperty("email")
    private String email;

    @JsonProperty("role")
    private Enums.UserRole role;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("gender")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Character gender;

    @JsonProperty("country")
    private String country;

    @JsonProperty("org_id")
    private String orgId;

    @JsonProperty("status")
    private Enums.UserStatus status;

    @JsonProperty("last_login_at")
    private LocalDateTime lastLoginAt;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
