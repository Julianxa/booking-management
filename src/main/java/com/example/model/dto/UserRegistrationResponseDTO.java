package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRegistrationResponseDTO {
    @JsonProperty("id")
    private String id;
    @JsonProperty("user_sub")
    private String userSub;
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    @JsonProperty("phone")
    private String phone;
    @JsonProperty("email")
    private String email;
    @JsonProperty("country")
    private String country;
    @JsonProperty("gender")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Character gender;
    @JsonProperty("org_id")
    private String orgId;
    @JsonProperty("status")
    private Enums.UserStatus status;
    @JsonProperty("session")
    private String session;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}

