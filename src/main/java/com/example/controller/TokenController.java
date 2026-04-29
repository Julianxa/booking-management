package com.example.controller;

import com.example.exception.InvalidEmailPasswordException;
import com.example.exception.UnverifiedEmailException;
import com.example.model.dto.ErrorResponseDTO;
import com.example.model.dto.*;
import com.example.service.TokenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import java.time.LocalDateTime;

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
        try {
            LoginResponseDTO response = tokenService.login(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (UnverifiedEmailException e) {
            return ResponseEntity.status(403).body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        } catch (InvalidEmailPasswordException e) {
            return ResponseEntity.status(400).body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        }
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
        try {
            TokenRenewalResponseDTO response = tokenService.refresh(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .message("Operation failed: " + e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        }
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
        try {
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("Missing or invalid X-Access-Token header");
            }
            LogoutResponseDTO response = tokenService.logout(accessToken);
            return ResponseEntity.ok(response);
        } catch(NotAuthorizedException e) {
            return ResponseEntity.status(401).body(
                    ErrorResponseDTO.builder()
                            .message("Operation failed: " + e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .message("Operation failed: " + e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        }
    }
}
