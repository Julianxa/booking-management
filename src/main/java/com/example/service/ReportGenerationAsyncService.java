package com.example.service;

import com.example.constant.Enums;
import com.example.model.entity.Reports;
import com.example.model.record.BookingsByActivityDateReportRow;
import com.example.model.record.CountryOfOriginReportRow;
import com.example.model.record.GiftCertificateUsedInBookingReportRow;
import com.example.model.record.PromoCodesByTransactionDateReportRow;
import com.example.repository.ReportDataRepository;
import com.example.repository.ReportsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationAsyncService {
  private static final String REPORT_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final ReportsRepository reportsRepository;
  private final ReportDataRepository reportDataRepository;
  private final BookingsByActivityDateExcelBuilder bookingsByActivityDateExcelBuilder;
  private final BookingsByPurchaseDateExcelBuilder bookingsByPurchaseDateExcelBuilder;
  private final PromoCodesByTransactionDateExcelBuilder promoCodesByTransactionDateExcelBuilder;
  private final CountryOfOriginExcelBuilder countryOfOriginExcelBuilder;
  private final AwsService awsService;

  @Async("reportExecutor")
  public void generateReportAsync(Long reportId) {
    Reports report = reportsRepository.findById(reportId).orElse(null);
    if (report == null) {
      log.warn("Skipping async report generation because report {} was not found", reportId);
      return;
    }

    try {
      markInProgress(reportId);

      switch (report.getReportType()) {
        case BOOKINGS_BY_ACTIVITY_DATE -> generateBookingsByActivityDateReport(reportId);
        case BOOKINGS_BY_PURCHASE_DATE -> generateBookingsByPurchaseDateReport(reportId);
        case PROMO_CODES_BY_TRANSACTION_DATE -> generatePromoCodesByTransactionDateReport(reportId);
        case COUNTRY_OF_ORIGIN -> generateCountryOfOriginReport(reportId);
      }
    } catch (Exception e) {
      log.error("Async report generation failed for report {}", reportId, e);
      markFailed(reportId, e.getMessage());
    }
  }

  @Transactional
  protected void markInProgress(Long reportId) {
    Reports report = reportsRepository.findById(reportId).orElseThrow();
    report.setStatus(Enums.ReportStatus.IN_PROGRESS);
    report.setCompletedAt(null);
    report.setErrorMessage(null);
    reportsRepository.save(report);
  }

  @Transactional
  protected void markFailed(Long reportId, String errorMessage) {
    Reports report = reportsRepository.findById(reportId).orElseThrow();
    report.setStatus(Enums.ReportStatus.FAILED);
    report.setErrorMessage(truncate(errorMessage));
    report.setCompletedAt(ZonedDateTime.now());
    reportsRepository.save(report);
  }

  @Transactional
  protected void generateBookingsByActivityDateReport(Long reportId) {
    Reports report = reportsRepository.findById(reportId).orElseThrow();

    long totalBookingEventsInRange =
        reportDataRepository.countBookingEventsInDateRange(report.getStartDate(), report.getEndDate());
    List<BookingsByActivityDateReportRow> rows =
        reportDataRepository.findBookingsByActivityDate(report.getStartDate(), report.getEndDate());
    List<Long> bookingEventIds =
        rows.stream().map(BookingsByActivityDateReportRow::bookingEventId).toList();
    Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantities =
        reportDataRepository.findTicketQuantitiesByBookingEventIds(bookingEventIds);

    byte[] workbookBytes =
        bookingsByActivityDateExcelBuilder.build(
            report.getStartDate(),
            report.getEndDate(),
            report.getGeneratedBy(),
            rows,
            ticketQuantities);

    String s3Key =
        "reports/bookings-by-activity-date/"
            + report.getStartDate()
            + "_to_"
            + report.getEndDate()
            + "_"
            + UUID.randomUUID()
            + ".xlsx";

    awsService.uploadBytes(s3Key, workbookBytes, REPORT_CONTENT_TYPE);

    report.setS3Key(s3Key);
    report.setIncludedBookingEvents(rows.size());
    report.setTotalBookingEventsInRange(totalBookingEventsInRange);
    report.setFileSizeBytes((long) workbookBytes.length);
    report.setStatus(Enums.ReportStatus.COMPLETED);
    report.setCompletedAt(ZonedDateTime.now());
    report.setErrorMessage(null);
    reportsRepository.save(report);
  }

  @Transactional
  protected void generateBookingsByPurchaseDateReport(Long reportId) {
    Reports report = reportsRepository.findById(reportId).orElseThrow();

    long totalBookingEventsInRange =
        reportDataRepository.countBookingEventsByPurchaseDateInRange(
            report.getStartDate(), report.getEndDate());
    List<BookingsByActivityDateReportRow> rows =
        reportDataRepository.findBookingsByPurchaseDate(report.getStartDate(), report.getEndDate());
    List<Long> bookingEventIds =
        rows.stream().map(BookingsByActivityDateReportRow::bookingEventId).toList();
    Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantities =
        reportDataRepository.findTicketQuantitiesByBookingEventIds(bookingEventIds);
    List<GiftCertificateUsedInBookingReportRow> giftCertificateRows =
        reportDataRepository.findGiftCertificatesUsedInBookingsByPurchaseDate(
            report.getStartDate(), report.getEndDate());

    byte[] workbookBytes =
        bookingsByPurchaseDateExcelBuilder.build(
            report.getStartDate(),
            report.getEndDate(),
            report.getGeneratedBy(),
            rows,
            ticketQuantities,
            giftCertificateRows);

    String s3Key =
        "reports/bookings-by-purchase-date/"
            + report.getStartDate()
            + "_to_"
            + report.getEndDate()
            + "_"
            + UUID.randomUUID()
            + ".xlsx";

    awsService.uploadBytes(s3Key, workbookBytes, REPORT_CONTENT_TYPE);

    report.setS3Key(s3Key);
    report.setIncludedBookingEvents(rows.size());
    report.setTotalBookingEventsInRange(totalBookingEventsInRange);
    report.setFileSizeBytes((long) workbookBytes.length);
    report.setStatus(Enums.ReportStatus.COMPLETED);
    report.setCompletedAt(ZonedDateTime.now());
    report.setErrorMessage(null);
    reportsRepository.save(report);
  }

  @Transactional
  protected void generatePromoCodesByTransactionDateReport(Long reportId) {
    Reports report = reportsRepository.findById(reportId).orElseThrow();

    long totalBookingEventsInRange =
        reportDataRepository.countPromoCodesByTransactionDateInRange(
            report.getStartDate(), report.getEndDate());
    List<PromoCodesByTransactionDateReportRow> rows =
        reportDataRepository.findPromoCodesByTransactionDate(report.getStartDate(), report.getEndDate());
    List<Long> bookingEventIds =
        rows.stream().map(PromoCodesByTransactionDateReportRow::bookingEventId).toList();
    Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantities =
        reportDataRepository.findTicketQuantitiesByBookingEventIds(bookingEventIds);

    byte[] workbookBytes =
        promoCodesByTransactionDateExcelBuilder.build(
            report.getStartDate(),
            report.getEndDate(),
            report.getGeneratedBy(),
            rows,
            ticketQuantities);

    String s3Key =
        "reports/promo-codes-by-transaction-date/"
            + report.getStartDate()
            + "_to_"
            + report.getEndDate()
            + "_"
            + UUID.randomUUID()
            + ".xlsx";

    awsService.uploadBytes(s3Key, workbookBytes, REPORT_CONTENT_TYPE);

    report.setS3Key(s3Key);
    report.setIncludedBookingEvents(rows.size());
    report.setTotalBookingEventsInRange(totalBookingEventsInRange);
    report.setFileSizeBytes((long) workbookBytes.length);
    report.setStatus(Enums.ReportStatus.COMPLETED);
    report.setCompletedAt(ZonedDateTime.now());
    report.setErrorMessage(null);
    reportsRepository.save(report);
  }

  @Transactional
  protected void generateCountryOfOriginReport(Long reportId) {
    Reports report = reportsRepository.findById(reportId).orElseThrow();

    long totalBookingsInRange =
        reportDataRepository.countDistinctBookingsWithCountryByActivityDateInRange(
            report.getStartDate(), report.getEndDate());
    List<CountryOfOriginReportRow> rows =
        reportDataRepository.findCountryOfOriginByActivityDate(
            report.getStartDate(), report.getEndDate());

    byte[] workbookBytes =
        countryOfOriginExcelBuilder.build(
            report.getStartDate(),
            report.getEndDate(),
            report.getGeneratedBy(),
            rows);

    String s3Key =
        "reports/country-of-origin/"
            + report.getStartDate()
            + "_to_"
            + report.getEndDate()
            + "_"
            + UUID.randomUUID()
            + ".xlsx";

    awsService.uploadBytes(s3Key, workbookBytes, REPORT_CONTENT_TYPE);

    report.setS3Key(s3Key);
    report.setIncludedBookingEvents(rows.size());
    report.setTotalBookingEventsInRange(totalBookingsInRange);
    report.setFileSizeBytes((long) workbookBytes.length);
    report.setStatus(Enums.ReportStatus.COMPLETED);
    report.setCompletedAt(ZonedDateTime.now());
    report.setErrorMessage(null);
    reportsRepository.save(report);
  }

  private String truncate(String errorMessage) {
    if (errorMessage == null || errorMessage.isBlank()) {
      return "Report generation failed";
    }
    return errorMessage.length() <= 2000 ? errorMessage : errorMessage.substring(0, 2000);
  }
}
