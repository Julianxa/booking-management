package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenRenewalResponseDTO {
    @JsonProperty("email")
    private String email;
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("expires_in")
    private Number expiresIn;
    @JsonProperty("id_token")
    private String idToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}

