package com.example.controller;

import com.example.model.dto.ErrorResponseDTO;
import com.example.exception.InvalidIdTokenException;
import com.example.exception.UnverifiedEmailException;
import com.example.model.dto.*;
import com.example.service.AwsService;
import com.example.service.UserService;
import com.example.utils.UserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.LimitExceededException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import java.time.LocalDateTime;

import static com.example.constant.Enums.UserRole.*;

@Tag(name = "Users", description = "User management APIs")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class UserController {
    private final UserService userService;
    private final UserUtils userUtils;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user in the specified Cognito user pool.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User registered successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserRegistrationResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request or decryption failed", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid API key or signature", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping(value="/users", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signUp(@RequestBody UserRegistrationRequestDTO clientUserRegistrationRequest) {
        try {
            UserRegistrationResponseDTO userRegistrationResponseDTO = userService.register(clientUserRegistrationRequest);
            return ResponseEntity.ok(userRegistrationResponseDTO);
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
            summary = "Confirm user registration with OTP",
            description = "Confirms a user’s registration in the Cognito user pool using a verification code sent via email.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User confirmed successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ConfirmUserRegistrationResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid or expired verification code, or decryption failed",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid API key or signature",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping(value="/users/confirmation", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirmUser(
            @RequestBody ConfirmUserRegistrationRequestDTO confirmSignUpRequest) {
        try {
            ConfirmUserRegistrationResponseDTO confirmSignUpResponseDTO = userService.confirmSignUp(confirmSignUpRequest);
            return ResponseEntity.ok(confirmSignUpResponseDTO);
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
            summary = "Initiate forgot password",
            description = "Send a verification code to the user’s email for password recovery.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password reset email sent successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ForgotPasswordResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid email or user not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ForgotPasswordResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid API key or signature",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/users/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO forgotPasswordRequest) {
        try {
            ForgotPasswordResponseDTO forgotPasswordResponseDTO = userService.forgotPassword(forgotPasswordRequest);
            return ResponseEntity.ok(forgotPasswordResponseDTO);
        } catch (UnverifiedEmailException e) {
            return ResponseEntity.status(403).body(
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
            summary = "Confirm password reset with OTP",
            description = "Confirm the initiation of password recovery.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Forgot password is confirmed successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ConfirmForgotPasswordResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid email or user not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ConfirmForgotPasswordResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid API key or signature",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/users/forgot-password-confirmation")
    public ResponseEntity<?> confirmForgotPassword(
            @RequestBody ConfirmForgotPasswordRequestDTO confirmForgotPasswordRequestDTO) {
        try {
            ConfirmForgotPasswordResponseDTO confirmForgotPasswordResponseDTO = userService.confirmForgotPassword(confirmForgotPasswordRequestDTO);
            return ResponseEntity.ok(confirmForgotPasswordResponseDTO);
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
            summary = "Change user password",
            description = "Changes the user's password after validating the request.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password changed successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ChangePasswordResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request or user not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid or expired access token",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "429", description = "Attempt limit exceeded",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/users/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "X-Access-Token", required = false) String accessToken,
            @RequestBody ChangePasswordRequestDTO changePasswordRequest) {
        try {
            ChangePasswordResponseDTO changePasswordResponseDTO = userService.changePassword(accessToken, changePasswordRequest);
            return ResponseEntity.ok(changePasswordResponseDTO);
        } catch(LimitExceededException e) {
            return ResponseEntity.status(429).body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        } catch(NotAuthorizedException e) {
            if(e.awsErrorDetails().errorMessage().contains("Access Token has expired") | e.awsErrorDetails().errorMessage().contains("Invalid Access Token")) {
                return ResponseEntity.status(401).body(
                        ErrorResponseDTO.builder()
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now().toString())
                                .build()
                );
            } else {
                return ResponseEntity.status(400).body(
                        ErrorResponseDTO.builder()
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now().toString())
                                .build()
                );
            }
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
            summary = "Reset user password",
            description = "Completes the password reset process after OTP verification.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Password reset successful",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ResetPasswordResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid username, password, or OTP session expired",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized request due to invalid API key or signature",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)
                            )
                    )
            }
    )
    @PostMapping("/users/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO resetPasswordRequestDTO) {
        try {
            ResetPasswordResponseDTO responseDTO = userService.resetPassword(resetPasswordRequestDTO);
            return ResponseEntity.ok(responseDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    ErrorResponseDTO.builder()
                            .message("Internal server error: " + e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        }
    }

    @Operation(
            summary = "Delete user account",
            description = "Deletes the user's account after validating the request.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User account deleted successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DeleteUserResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request or user not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid API key or signature",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @DeleteMapping("/users/delete")
    public ResponseEntity<?> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Access-Token", required = false) String accessToken,
            @RequestBody DeleteUserRequestDTO deleteUserRequest) {
        try {
            String userSub = userUtils.extractUserSub(authorizationHeader);
            DeleteUserResponseDTO deleteUserResponseDTO = userService.deleteUser(userSub, accessToken, deleteUserRequest);
            return ResponseEntity.ok(deleteUserResponseDTO);
        } catch (NotAuthorizedException e) {
            if(e.awsErrorDetails().errorMessage().contains("Access Token has expired") | e.awsErrorDetails().errorMessage().contains("Invalid Access Token")) {
                return ResponseEntity.status(401).body(
                        ErrorResponseDTO.builder()
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now().toString())
                                .build()
                );
            } else {
                return ResponseEntity.status(400).body(
                        ErrorResponseDTO.builder()
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now().toString())
                                .build()
                );
            }
        } catch(InvalidIdTokenException e) {
            return ResponseEntity.status(401).body(
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
            summary = "Delete user by ID",
            description = "Deletes a user account by user ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUserById(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        try {
            DeleteUserResponseDTO response = userService.deleteUserById(userId);
            return ResponseEntity.ok(response);

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
            summary = "List all admin users",
            description = "Retrieves a paginated list of all users. Admin access required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users",
                            content = @Content(schema = @Schema(implementation = GetListUserResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
            }
    )
    @GetMapping("/users/admins")
    public ResponseEntity<?> getAllAdminUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String search) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            GetListUserResponseDTO users = userService.getAllUsers(pageable, search, ADMIN, null);
            return ResponseEntity.ok(users);
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
            summary = "List all employee users",
            description = "Retrieves a paginated list of all users. Admin access required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users",
                            content = @Content(schema = @Schema(implementation = GetListUserResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
            }
    )
    @GetMapping("/users/employees")
    public ResponseEntity<?> getAllEmployeeUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String search) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            GetListUserResponseDTO users = userService.getAllUsers(pageable, search, EMPLOYEE, null);
            return ResponseEntity.ok(users);
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
            summary = "List all agent users",
            description = "Retrieves a paginated list of all users. Admin access required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users",
                            content = @Content(schema = @Schema(implementation = GetListUserResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
            }
    )
    @GetMapping("/users/agents")
    public ResponseEntity<?> getAllAgentUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String orgId) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            GetListUserResponseDTO users = userService.getAllUsers(pageable, search, AGENT, orgId);
            return ResponseEntity.ok(users);
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
            summary = "Get public user by ID",
            description = "Retrieves detailed user information by ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(schema = @Schema(implementation = GetUserResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
            }
    )
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable String id) {

        try {
            GetUserResponseDTO user = userService.getUserByIdAndRole(id, USER);
            return ResponseEntity.ok(user);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
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
            summary = "List all public users",
            description = "Retrieves a paginated list of all users. Admin access required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users",
                            content = @Content(schema = @Schema(implementation = GetListUserResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
            }
    )
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String search) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            GetListUserResponseDTO users = userService.getAllUsers(pageable, search, USER, null);
            return ResponseEntity.ok(users);
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
            summary = "Update user information",
            description = "Allows to update user attributes (name, email, etc.). ",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User updated successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetUserResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized or invalid token",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Forbidden - insufficient permissions",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUserByAdmin(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequestDTO dto) {
        try {
            GetUserResponseDTO updatedUser = userService.updateUserByAdmin(id, dto);
            return ResponseEntity.ok(updatedUser);
        } catch (ResponseStatusException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .message(e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponseDTO.builder()
                            .message("Internal server error: " + e.getMessage())
                            .timestamp(LocalDateTime.now().toString())
                            .build()
            );
        }
    }

    @Operation(
            summary = "Get Admin user by ID",
            description = "Retrieves detailed admin user information by ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(schema = @Schema(implementation = GetUserResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
            }
    )
    @GetMapping("/users/admins/{id}")
    public ResponseEntity<?> getAdminUserById(
            @PathVariable String id) {

        try {
            GetUserResponseDTO user = userService.getUserByIdAndRole(id, ADMIN);
            return ResponseEntity.ok(user);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
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
            summary = "Get Agent user by ID",
            description = "Retrieves detailed agent user information by ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(schema = @Schema(implementation = GetUserResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
            }
    )
    @GetMapping("/users/agents/{id}")
    public ResponseEntity<?> getAgentUserById(
            @PathVariable String id) {

        try {
            GetUserResponseDTO user = userService.getUserByIdAndRole(id, AGENT);
            return ResponseEntity.ok(user);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
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
            summary = "Get Employee user by ID",
            description = "Retrieves detailed employee user information by ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(schema = @Schema(implementation = GetUserResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
            }
    )
    @GetMapping("/users/employees/{id}")
    public ResponseEntity<?> getEmployeeUserById(
            @PathVariable String id) {

        try {
            GetUserResponseDTO user = userService.getUserByIdAndRole(id, EMPLOYEE);
            return ResponseEntity.ok(user);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
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
            summary = "Get current user by user sub",
            description = "Retrieves current user information by user sub.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(schema = @Schema(implementation = GetUserResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - not admin")
            }
    )
    @GetMapping("/users/currentUser")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
                                                             @RequestHeader(value = "X-Access-Token", required = false) String accessToken) {
        try {
            String userSub = userUtils.extractUserSub(authorizationHeader);
            GetUserResponseDTO userProfile = userService.getUserByUserSub(userSub);
            return ResponseEntity.ok(userProfile);
        } catch (NotAuthorizedException e) {
            if (e.awsErrorDetails().errorMessage().contains("Access Token has expired") | e.awsErrorDetails().errorMessage().contains("Invalid Access Token")) {
                return ResponseEntity.status(401).body(
                        ErrorResponseDTO.builder()
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now().toString())
                                .build()
                );
            } else {
                return ResponseEntity.status(400).body(
                        ErrorResponseDTO.builder()
                                .message(e.getMessage())
                                .timestamp(LocalDateTime.now().toString())
                                .build()
                );
            }
        } catch (InvalidIdTokenException e) {
            return ResponseEntity.status(401).body(
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
}