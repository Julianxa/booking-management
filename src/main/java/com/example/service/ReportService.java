package com.example.service;

import com.example.constant.Enums;
import com.example.exception.BusinessException;
import com.example.exception.report.ReportNotFoundException;
import com.example.exception.ErrorCode;
import com.example.mapper.ReportMapper;
import com.example.model.dto.DeleteReportResponseDTO;
import com.example.model.dto.GenerateBookingsByActivityDateReportRequestDTO;
import com.example.model.dto.GenerateBookingsByActivityDateReportResponseDTO;
import com.example.model.dto.GetListReportsResponseDTO;
import com.example.model.dto.ReportSummaryResponseDTO;
import com.example.model.entity.Reports;
import com.example.model.record.BookingsByActivityDateReportRow;
import com.example.model.record.PromoCodesByTransactionDateReportRow;
import com.example.repository.ReportDataRepository;
import com.example.repository.ReportsRepository;
import com.example.utils.ReferenceNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {
  private static final String REPORT_CONTENT_TYPE =
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private final ReportDataRepository reportDataRepository;
  private final ReportsRepository reportsRepository;
  private final BookingsByActivityDateExcelBuilder excelBuilder;
  private final PromoCodesByTransactionDateExcelBuilder promoCodesByTransactionDateExcelBuilder;
  private final AwsService awsService;
  private final ReferenceNoGenerator referenceNoGenerator;
  private final ReportMapper reportMapper;

  @Transactional(readOnly = true)
  public ReportData loadBookingsByActivityDateReportData(
      LocalDate startDate, LocalDate endDate) {
    long totalBookingEventsInRange =
        reportDataRepository.countBookingEventsInDateRange(startDate, endDate);

    List<BookingsByActivityDateReportRow> rows =
        reportDataRepository.findBookingsByActivityDate(startDate, endDate);

    List<Long> bookingEventIds =
        rows.stream().map(BookingsByActivityDateReportRow::bookingEventId).toList();
    Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantities =
        reportDataRepository.findTicketQuantitiesByBookingEventIds(bookingEventIds);

    return new ReportData(rows, ticketQuantities, totalBookingEventsInRange);
  }

  @Transactional
  public GenerateBookingsByActivityDateReportResponseDTO generateBookingsByActivityDateReport(
      GenerateBookingsByActivityDateReportRequestDTO request) {
    LocalDate startDate = request.getStartDate();
    LocalDate endDate = request.getEndDate();

    if (startDate.isAfter(endDate)) {
      throw new BusinessException(
          ErrorCode.MISSING_REQUIRED_FIELD, "start_date must be on or before end_date");
    }

    ReportData reportData = loadBookingsByActivityDateReportData(startDate, endDate);

    byte[] workbookBytes =
        excelBuilder.build(
            startDate,
            endDate,
            request.getGeneratedBy(),
            reportData.rows(),
            reportData.ticketQuantities());

    String s3Key =
        "reports/bookings-by-activity-date/"
            + startDate
            + "_to_"
            + endDate
            + "_"
            + UUID.randomUUID()
            + ".xlsx";

    awsService.uploadBytes(s3Key, workbookBytes, REPORT_CONTENT_TYPE);
    String downloadUrl = awsService.getFileFromS3(s3Key, Duration.ofHours(1));

    Reports savedReport =
        reportsRepository.save(
            Reports.builder()
                .refNo(referenceNoGenerator.generateReportReference())
                .reportType(Enums.ReportType.BOOKINGS_BY_ACTIVITY_DATE)
                .s3Key(s3Key)
                .startDate(startDate)
                .endDate(endDate)
                .generatedBy(request.getGeneratedBy())
                .includedBookingEvents(reportData.rows().size())
                .totalBookingEventsInRange(reportData.totalBookingEventsInRange())
                .fileSizeBytes((long) workbookBytes.length)
                .build());

    return GenerateBookingsByActivityDateReportResponseDTO.builder()
        .id(savedReport.getRefNo())
        .s3Key(s3Key)
        .downloadUrl(downloadUrl)
        .reportStartDate(startDate)
        .reportEndDate(endDate)
        .includedBookingEvents(reportData.rows().size())
        .totalBookingEventsInRange(reportData.totalBookingEventsInRange())
        .message(buildReportMessage(reportData, startDate, endDate))
        .timestamp(ZonedDateTime.now())
        .build();
  }

  @Transactional(readOnly = true)
  public PromoCodesReportData loadPromoCodesByTransactionDateReportData(
      LocalDate startDate, LocalDate endDate) {
    long totalBookingEventsInRange =
        reportDataRepository.countPromoCodesByTransactionDateInRange(startDate, endDate);

    List<PromoCodesByTransactionDateReportRow> rows =
        reportDataRepository.findPromoCodesByTransactionDate(startDate, endDate);

    List<Long> bookingEventIds =
        rows.stream().map(PromoCodesByTransactionDateReportRow::bookingEventId).toList();
    Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantities =
        reportDataRepository.findTicketQuantitiesByBookingEventIds(bookingEventIds);

    return new PromoCodesReportData(rows, ticketQuantities, totalBookingEventsInRange);
  }

  @Transactional
  public GenerateBookingsByActivityDateReportResponseDTO generatePromoCodesByTransactionDateReport(
      GenerateBookingsByActivityDateReportRequestDTO request) {
    LocalDate startDate = request.getStartDate();
    LocalDate endDate = request.getEndDate();

    if (startDate.isAfter(endDate)) {
      throw new BusinessException(
          ErrorCode.MISSING_REQUIRED_FIELD, "start_date must be on or before end_date");
    }

    PromoCodesReportData reportData = loadPromoCodesByTransactionDateReportData(startDate, endDate);

    byte[] workbookBytes =
        promoCodesByTransactionDateExcelBuilder.build(
            startDate,
            endDate,
            request.getGeneratedBy(),
            reportData.rows(),
            reportData.ticketQuantities());

    String s3Key =
        "reports/promo-codes-by-transaction-date/"
            + startDate
            + "_to_"
            + endDate
            + "_"
            + UUID.randomUUID()
            + ".xlsx";

    awsService.uploadBytes(s3Key, workbookBytes, REPORT_CONTENT_TYPE);
    String downloadUrl = awsService.getFileFromS3(s3Key, Duration.ofHours(1));

    Reports savedReport =
        reportsRepository.save(
            Reports.builder()
                .refNo(referenceNoGenerator.generateReportReference())
                .reportType(Enums.ReportType.PROMO_CODES_BY_TRANSACTION_DATE)
                .s3Key(s3Key)
                .startDate(startDate)
                .endDate(endDate)
                .generatedBy(request.getGeneratedBy())
                .includedBookingEvents(reportData.rows().size())
                .totalBookingEventsInRange(reportData.totalBookingEventsInRange())
                .fileSizeBytes((long) workbookBytes.length)
                .build());

    return GenerateBookingsByActivityDateReportResponseDTO.builder()
        .id(savedReport.getRefNo())
        .s3Key(s3Key)
        .downloadUrl(downloadUrl)
        .reportStartDate(startDate)
        .reportEndDate(endDate)
        .includedBookingEvents(reportData.rows().size())
        .totalBookingEventsInRange(reportData.totalBookingEventsInRange())
        .message(buildPromoCodesReportMessage(reportData, startDate, endDate))
        .timestamp(ZonedDateTime.now())
        .build();
  }

  @Transactional(readOnly = true)
  public GetListReportsResponseDTO getAllReports(
      Pageable pageable, Enums.ReportType reportType) {
    Page<Reports> reportsPage =
        reportType == null
            ? reportsRepository.findAll(pageable)
            : reportsRepository.findByReportType(reportType, pageable);

    List<ReportSummaryResponseDTO> content =
        reportsPage.getContent().stream().map(this::toSummaryWithDownloadUrl).toList();

    Page<ReportSummaryResponseDTO> mappedPage =
        new PageImpl<>(content, pageable, reportsPage.getTotalElements());
    return reportMapper.toGetListResponse(mappedPage);
  }

  @Transactional(readOnly = true)
  public ReportSummaryResponseDTO getReportByRefNo(String refNo) {
    Reports report =
        reportsRepository
            .findByRefNo(refNo)
            .orElseThrow(
                () -> new ReportNotFoundException(String.format("Report %s not found", refNo)));
    return toSummaryWithDownloadUrl(report);
  }

  @Transactional
  public DeleteReportResponseDTO deleteReportByRefNo(String refNo) {
    Reports report =
        reportsRepository
            .findByRefNo(refNo)
            .orElseThrow(
                () -> new ReportNotFoundException(String.format("Report %s not found", refNo)));

    if (report.getS3Key() != null && !report.getS3Key().isBlank()) {
      awsService.deleteFile(report.getS3Key());
    }

    reportsRepository.delete(report);

    DeleteReportResponseDTO response = new DeleteReportResponseDTO();
    response.setMessage("Report deleted successfully");
    response.setTimestamp(ZonedDateTime.now());
    return response;
  }

  private ReportSummaryResponseDTO toSummaryWithDownloadUrl(Reports report) {
    ReportSummaryResponseDTO summary = reportMapper.toSummaryResponseDTO(report);
    if (report.getS3Key() != null && !report.getS3Key().isBlank()) {
      summary.setDownloadUrl(awsService.getFileFromS3(report.getS3Key(), Duration.ofHours(1)));
    }
    return summary;
  }

  private String buildReportMessage(ReportData reportData, LocalDate startDate, LocalDate endDate) {
    if (!reportData.rows().isEmpty()) {
      return "Bookings by activity date report generated successfully";
    }
    if (reportData.totalBookingEventsInRange() == 0) {
      return String.format(
          "Report generated with no rows. No booking events found with activity date (event_date) "
              + "between %s and %s. Use the event/activity date, not the purchase date.",
          startDate, endDate);
    }
    return String.format(
        "Report generated with no rows. Found %d booking event(s) in the date range, but none "
            + "matched report filters (booking event not cancelled; booking status not "
            + "CANCELLED/FAILED/EXPIRED/REFUNDED).",
        reportData.totalBookingEventsInRange());
  }

  private String buildPromoCodesReportMessage(
      PromoCodesReportData reportData, LocalDate startDate, LocalDate endDate) {
    if (!reportData.rows().isEmpty()) {
      return "Promo codes by transaction date report generated successfully";
    }
    if (reportData.totalBookingEventsInRange() == 0) {
      return String.format(
          "Report generated with no rows. No promo code bookings found with payment transaction date "
              + "between %s and %s.",
          startDate, endDate);
    }
    return String.format(
        "Report generated with no rows. Found %d promo booking event(s) in the transaction date "
            + "range, but none matched report filters (booking event not cancelled; booking status "
            + "not CANCELLED/FAILED/EXPIRED/REFUNDED; promo redemption SUCCESS; payment SUCCEEDED).",
        reportData.totalBookingEventsInRange());
  }

  private record ReportData(
      List<BookingsByActivityDateReportRow> rows,
      Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantities,
      long totalBookingEventsInRange) {}

  private record PromoCodesReportData(
      List<PromoCodesByTransactionDateReportRow> rows,
      Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantities,
      long totalBookingEventsInRange) {}
}
