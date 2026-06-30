package com.example.controller;

import com.example.model.dto.ErrorResponseDTO;
import com.example.model.dto.GenerateBookingsByActivityDateReportRequestDTO;
import com.example.model.dto.GenerateBookingsByActivityDateReportResponseDTO;
import com.example.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports", description = "Report generation APIs")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {
  private final ReportService reportService;

  @Operation(
      summary = "Generate bookings by activity date report",
      description =
          "Builds an Excel report for all paid online bookings within the activity date range, "
              + "uploads it to S3, and returns a presigned download URL.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Report generated",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema =
                        @Schema(
                            implementation =
                                GenerateBookingsByActivityDateReportResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
      })
  @PostMapping("/reports/bookings-by-activity-date")
  public ResponseEntity<GenerateBookingsByActivityDateReportResponseDTO>
      generateBookingsByActivityDateReport(
          @Valid @RequestBody GenerateBookingsByActivityDateReportRequestDTO request) {
    return ResponseEntity.ok(reportService.generateBookingsByActivityDateReport(request));
  }
}
