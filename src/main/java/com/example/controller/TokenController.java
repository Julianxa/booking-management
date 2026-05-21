package com.example.controller;

import com.example.model.dto.*;
import com.example.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tokens", description = "Token management APIs")
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@RestController
public class TokenController {
    private final TokenService tokenService;

    @Operation(
            summary = "Initiate authentication",
            description = "Authenticates a user in the Cognito user pool.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tokens generated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = LoginResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request or credentials",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/tokens")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO request, HttpServletRequest httpRequest) {
        LoginResponseDTO response = tokenService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh ID and access tokens",
            description = "Refreshes ID and access tokens using a valid refresh token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TokenRenewalResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request or refresh token",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/tokens/renewal")
    public ResponseEntity<?> refreshTokens(@RequestBody @Valid TokenRenewalRequestDTO request) {
        TokenRenewalResponseDTO response = tokenService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Revoke authentication tokens",
            description = "Invalidates a user’s session using the access token provided in the X-Access-Token header. Secured with HTTPS.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tokens revoked successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = LogoutResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing access token",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping(value = "/tokens/revocation")
    public ResponseEntity<?> revokeTokens(
            @RequestHeader(value = "X-Access-Token", required = false) String accessToken) {
        LogoutResponseDTO response = tokenService.logout(accessToken);
        return ResponseEntity.ok(response);
    }
}
