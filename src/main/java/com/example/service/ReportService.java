package com.example.service;

import com.example.exception.BusinessException;
import com.example.exception.ErrorCode;
import com.example.model.dto.GenerateBookingsByActivityDateReportRequestDTO;
import com.example.model.dto.GenerateBookingsByActivityDateReportResponseDTO;
import com.example.model.record.BookingsByActivityDateReportRow;
import com.example.repository.ReportDataRepository;
import lombok.RequiredArgsConstructor;
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
  private final BookingsByActivityDateExcelBuilder excelBuilder;
  private final AwsService awsService;

  @Transactional(readOnly = true)
  public ReportData loadBookingsByActivityDateReportData(
      LocalDate startDate, LocalDate endDate) {
    long bookingEventsInRange =
        reportDataRepository.countBookingEventsInDateRange(startDate, endDate);

    List<BookingsByActivityDateReportRow> rows =
        reportDataRepository.findBookingsByActivityDate(startDate, endDate);

    List<Long> bookingEventIds =
        rows.stream().map(BookingsByActivityDateReportRow::bookingEventId).toList();
    Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantities =
        reportDataRepository.findTicketQuantitiesByBookingEventIds(bookingEventIds);

    return new ReportData(rows, ticketQuantities, bookingEventsInRange);
  }

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

    return GenerateBookingsByActivityDateReportResponseDTO.builder()
        .s3Key(s3Key)
        .downloadUrl(downloadUrl)
        .reportStartDate(startDate)
        .reportEndDate(endDate)
        .rowCount(reportData.rows().size())
        .bookingEventsInRange(reportData.bookingEventsInRange())
        .message(buildReportMessage(reportData, startDate, endDate))
        .timestamp(ZonedDateTime.now())
        .build();
  }

  private String buildReportMessage(ReportData reportData, LocalDate startDate, LocalDate endDate) {
    if (!reportData.rows().isEmpty()) {
      return "Bookings by activity date report generated successfully";
    }
    if (reportData.bookingEventsInRange() == 0) {
      return String.format(
          "Report generated with no rows. No booking events found with activity date (event_date) "
              + "between %s and %s. Use the event/activity date, not the purchase date.",
          startDate, endDate);
    }
    return String.format(
        "Report generated with no rows. Found %d booking event(s) in the date range, but none "
            + "matched report filters (booking event not cancelled; booking status not "
            + "CANCELLED/FAILED/EXPIRED/REFUNDED).",
        reportData.bookingEventsInRange());
  }

  private record ReportData(
      List<BookingsByActivityDateReportRow> rows,
      Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantities,
      long bookingEventsInRange) {}
}
