package com.example.controller;

import com.example.model.dto.DeleteReportResponseDTO;
import com.example.model.dto.ErrorResponseDTO;
import com.example.model.dto.GenerateBookingsByActivityDateReportRequestDTO;
import com.example.model.dto.GenerateBookingsByActivityDateReportResponseDTO;
import com.example.model.dto.GetListReportsResponseDTO;
import com.example.model.dto.ReportSummaryResponseDTO;
import com.example.service.ReportService;
import com.example.constant.Enums;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @Operation(
      summary = "Generate promo codes by transaction date report",
      description =
          "Builds an Excel report for bookings that redeemed VALUE, EVENT, PERSONAL_VALUE, or "
              + "PERSONAL_EVENT promo codes within the transaction date range, uploads it to S3, "
              + "and returns a presigned download URL.",
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
  @PostMapping("/reports/promo-codes-by-transaction-date")
  public ResponseEntity<GenerateBookingsByActivityDateReportResponseDTO>
      generatePromoCodesByTransactionDateReport(
          @Valid @RequestBody GenerateBookingsByActivityDateReportRequestDTO request) {
    return ResponseEntity.ok(reportService.generatePromoCodesByTransactionDateReport(request));
  }

  @Operation(
      summary = "List generated reports",
      description = "Returns a paginated list of stored reports with presigned download URLs.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "List of reports",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = GetListReportsResponseDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
      })
  @GetMapping("/reports")
  public ResponseEntity<GetListReportsResponseDTO> getAllReports(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(value = "sort_by", defaultValue = "created_at") String sortBy,
      @RequestParam(value = "direction", defaultValue = "DESC") Sort.Direction direction,
      @RequestParam(value = "report_type", required = false) Enums.ReportType reportType) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, mapReportSortField(sortBy)));
    return ResponseEntity.ok(reportService.getAllReports(pageable, reportType));
  }

  @Operation(
      summary = "Get report by ID",
      description = "Returns a stored report by its reference number with a presigned download URL.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Report found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportSummaryResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Report not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
      })
  @GetMapping("/reports/{id}")
  public ResponseEntity<ReportSummaryResponseDTO> getReport(@PathVariable String id) {
    return ResponseEntity.ok(reportService.getReportByRefNo(id));
  }

  @Operation(
      summary = "Delete report by ID",
      description =
          "Deletes a stored report by its reference number and removes the file from S3.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Report deleted successfully",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DeleteReportResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Report not found",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
      })
  @DeleteMapping("/reports/{id}")
  public ResponseEntity<DeleteReportResponseDTO> deleteReport(@PathVariable String id) {
    return ResponseEntity.ok(reportService.deleteReportByRefNo(id));
  }

  private static String mapReportSortField(String sortBy) {
    return switch (sortBy) {
      case "id" -> "refNo";
      case "report_start_date" -> "startDate";
      case "report_end_date" -> "endDate";
      case "created_at" -> "createdAt";
      case "report_type" -> "reportType";
      case "generated_by" -> "generatedBy";
      case "included_booking_events" -> "includedBookingEvents";
      case "total_booking_events_in_range" -> "totalBookingEventsInRange";
      case "file_size_bytes" -> "fileSizeBytes";
      default -> sortBy;
    };
  }
}
