package com.example.controller;

import com.example.model.dto.*;
import com.example.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Organizations", description = "Organization management APIs")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class OrganizationController {
    private final OrganizationService organizationService;

    @Operation(
            summary = "Create a new organization",
            description = "Creates a new organization record.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Organization created successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateOrganizationResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request data",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/organizations")
    public ResponseEntity<?> create(@RequestBody CreateOrganizationRequestDTO dto) {
        return ResponseEntity.ok(organizationService.createOrganization(dto));
    }

    @Operation(
            summary = "Get organization by ID",
            description = "Retrieves details of a specific organization.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Organization found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateOrganizationResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Organization not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/organizations/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id) {
        CreateOrganizationResponseDTO organizationResponseDTO = organizationService.getOrganization(id);
        return ResponseEntity.ok(organizationResponseDTO);
    }

    @Operation(
            summary = "List all organizations",
            description = "Returns a list of all organizations.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "List of organizations",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = GetListOrganizationResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/organizations")
    public ResponseEntity<?> getAll(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(value = "sort_by", defaultValue = "id") String sortBy,
                                    @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,
                                    @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        GetListOrganizationResponseDTO organizations = organizationService.getAllOrganizations(pageable, search);

        return ResponseEntity.ok(organizations);
    }

    @Operation(
            summary = "Update an organization",
            description = "Updates an existing organization (partial update).",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Organization updated successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UpdateOrganizationResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Organization not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PatchMapping("/organizations/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                    @RequestBody UpdateOrganizationRequestDTO dto) {
        UpdateOrganizationResponseDTO organizationResponseDTO = organizationService.updateOrganization(id, dto);
        return ResponseEntity.ok(organizationResponseDTO);
    }

    @Operation(
            summary = "Delete an organization",
            description = "Soft deletes an organization by ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Organization deleted successfully"),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Organization not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @DeleteMapping("/organizations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<?> delete(@PathVariable String id) {
        DeleteOrganizationResponseDTO deleteOrganizationResponseDTO = organizationService.deleteOrganization(id);
        return ResponseEntity.ok(deleteOrganizationResponseDTO);
    }
}
