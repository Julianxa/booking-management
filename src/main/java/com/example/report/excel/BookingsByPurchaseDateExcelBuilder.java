package com.example.report.excel;

import com.example.exception.general.FileOperationException;
import com.example.model.record.BookingsByActivityDateReportRow;
import com.example.model.record.GiftCertificateUsedInBookingReportRow;
import com.example.repository.ReportDataRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class BookingsByPurchaseDateExcelBuilder {
  private static final String DEFAULT_COMPANY = "Hong Kong Tramways";
  private static final String INTERNET_USER = "Internet User";
  private static final String DEFAULT_SALES_CHANNEL = "Default";
  private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Hong_Kong");

  private static final DateTimeFormatter REPORT_DATE_FORMAT =
      DateTimeFormatter.ofPattern("EEEE, MMM d yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter REPORT_DAY_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DATE_CREATED_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter REPORT_GENERATED_ON_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter ACTIVITY_TIME_FORMAT =
      DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);

  private static final String[] ACTIVITY_HEADERS = {
    "Booking No.",
    "Date Created",
    "Activity",
    "Date",
    "Sales Channel",
    "Days to Purchase",
    "Company",
    "Agent",
    "Guest",
    "Passengers",
    "Sub-Total",
    "Discount(s)",
    "Total",
    "Net Total"
  };

  private static final String[] GIFT_CERTIFICATE_HEADERS = {
    "Booking No.",
    "Date",
    "Gift Certificate Id",
    "Recipient",
    "Message to Recipient",
    "Total"
  };

  public byte[] build(
      LocalDate startDate,
      LocalDate endDate,
      String generatedBy,
      List<BookingsByActivityDateReportRow> rows,
      Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantitiesByBookingEventId,
      List<GiftCertificateUsedInBookingReportRow> giftCertificateRows) {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      String sheetName =
          startDate + "-Report-" + UUID.randomUUID().toString().replace("-", "").substring(0, 13);
      Sheet sheet = workbook.createSheet(sheetName.substring(0, Math.min(sheetName.length(), 31)));

      ZonedDateTime generatedAt = ZonedDateTime.now(REPORT_ZONE);
      CellStyle sectionTitleStyle = createSectionTitleStyle(workbook);
      int rowIndex =
          writeMetadata(sheet, startDate, endDate, generatedBy, generatedAt, sectionTitleStyle);

      writeHeaderRow(sheet, rowIndex++, ACTIVITY_HEADERS);

      ActivityTotals totals = new ActivityTotals();

      for (BookingsByActivityDateReportRow row : rows) {
        List<ReportDataRepository.TicketQuantityRow> ticketRows =
            ticketQuantitiesByBookingEventId.getOrDefault(row.bookingEventId(), List.of());
        writeActivityDataRow(sheet, rowIndex++, row, ticketRows, totals);
      }

      rowIndex = writeActivityTotalRows(sheet, rowIndex, totals);
      writeGiftCertificateSummarySection(sheet, rowIndex, sectionTitleStyle, giftCertificateRows);

      int maxColumns =
          Math.max(ACTIVITY_HEADERS.length, GIFT_CERTIFICATE_HEADERS.length);
      for (int columnIndex = 0; columnIndex < maxColumns; columnIndex++) {
        sheet.autoSizeColumn(columnIndex);
      }

      workbook.write(outputStream);
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new FileOperationException("Failed to generate bookings by purchase date report");
    }
  }

  private int writeMetadata(
      Sheet sheet,
      LocalDate startDate,
      LocalDate endDate,
      String generatedBy,
      ZonedDateTime generatedAt,
      CellStyle sectionTitleStyle) {
    int row = 2;
    createLabelValueRow(sheet, row++, "ALL INTERNET BOOKINGS BY DATE CREATED", null);
    row++;
    createLabelValueRow(sheet, row++, "Report Start Date:", startDate.format(REPORT_DATE_FORMAT));
    createLabelValueRow(sheet, row++, "Report End Date:", endDate.format(REPORT_DATE_FORMAT));
    createLabelValueRow(
        sheet,
        row++,
        "Report Generated On:",
        generatedAt.withZoneSameInstant(REPORT_ZONE).format(REPORT_GENERATED_ON_FORMAT));
    createLabelValueRow(
        sheet, row++, "Generated By:", generatedBy != null ? generatedBy : "System");
    row++;
    createSectionTitleRow(sheet, row++, "Activity Summary", sectionTitleStyle);
    return row;
  }

  private void writeHeaderRow(Sheet sheet, int rowIndex, String[] headers) {
    Row row = sheet.createRow(rowIndex);
    for (int i = 0; i < headers.length; i++) {
      row.createCell(i).setCellValue(headers[i]);
    }
  }

  private void writeActivityDataRow(
      Sheet sheet,
      int rowIndex,
      BookingsByActivityDateReportRow row,
      List<ReportDataRepository.TicketQuantityRow> ticketRows,
      ActivityTotals totals) {
    BigDecimal subtotal = nullToZero(row.eventSubtotal());
    BigDecimal discount = allocatedDiscount(row);
    BigDecimal total = subtotal.subtract(discount);
    BigDecimal daysToPurchase = BigDecimal.valueOf(daysToPurchase(row));

    Row excelRow = sheet.createRow(rowIndex);
    int column = 0;
    setTextCell(excelRow, column++, row.bookingRefNo());
    setDateCreatedCell(excelRow, column++, row.purchaseDate());
    excelRow.createCell(column++).setCellValue(formatActivityTime(row.eventTime()));
    setDateCell(excelRow, column++, row.eventDate());
    excelRow.createCell(column++).setCellValue(DEFAULT_SALES_CHANNEL);
    excelRow.createCell(column++).setCellValue(daysToPurchase.doubleValue());
    excelRow.createCell(column++).setCellValue(DEFAULT_COMPANY);
    excelRow.createCell(column++).setCellValue(INTERNET_USER);
    excelRow.createCell(column++).setCellValue(formatGuestName(row));
    excelRow.createCell(column++).setCellValue(countPassengers(ticketRows));
    setMoneyCell(excelRow, column++, subtotal);
    setMoneyCell(excelRow, column++, discount);
    setMoneyCell(excelRow, column++, total);
    setMoneyCell(excelRow, column++, total);

    totals.add(daysToPurchase, ticketRows, subtotal, discount, total);
  }

  private int writeActivityTotalRows(Sheet sheet, int rowIndex, ActivityTotals totals) {
    Row row = sheet.createRow(rowIndex++);
    row.createCell(0).setCellValue("Total");
    row.createCell(5).setCellValue("Avg: " + totals.averageDaysToPurchase());
    row.createCell(9).setCellValue(totals.totalPassengers);
    setMoneyCell(row, 10, totals.subtotal);
    setMoneyCell(row, 11, totals.discount);
    setMoneyCell(row, 12, totals.total);
    setMoneyCell(row, 13, totals.total);

    Row medianRow = sheet.createRow(rowIndex++);
    medianRow.createCell(5).setCellValue("Median: " + totals.medianDaysToPurchase());
    return rowIndex;
  }

  private void writeGiftCertificateSummarySection(
      Sheet sheet,
      int rowIndex,
      CellStyle sectionTitleStyle,
      List<GiftCertificateUsedInBookingReportRow> rows) {
    rowIndex++;
    createSectionTitleRow(
        sheet, rowIndex++, "Purchased Gift Certificates Summary", sectionTitleStyle);
    writeHeaderRow(sheet, rowIndex++, GIFT_CERTIFICATE_HEADERS);

    BigDecimal totalDiscount = BigDecimal.ZERO;
    for (GiftCertificateUsedInBookingReportRow row : rows) {
      writeGiftCertificateDataRow(sheet, rowIndex++, row);
      totalDiscount = totalDiscount.add(nullToZero(row.discount()));
    }

    if (!rows.isEmpty()) {
      Row totalRow = sheet.createRow(rowIndex);
      totalRow.createCell(0).setCellValue("Total");
      setMoneyCell(totalRow, 5, totalDiscount);
    }
  }

  private void writeGiftCertificateDataRow(
      Sheet sheet, int rowIndex, GiftCertificateUsedInBookingReportRow row) {
    Row excelRow = sheet.createRow(rowIndex);
    int column = 0;
    setTextCell(excelRow, column++, row.bookingRefNo());
    if (row.purchaseDateTime() != null) {
      setDateCell(
          excelRow, column++, row.purchaseDateTime().withZoneSameInstant(REPORT_ZONE).toLocalDate());
    } else {
      excelRow.createCell(column++).setBlank();
    }
    setTextCell(excelRow, column++, row.giftCertificateRefNo());
    setTextCell(excelRow, column++, row.recipient());
    setTextCell(excelRow, column++, row.messageToRecipient());
    setMoneyCell(excelRow, column, row.discount());
  }

  private CellStyle createSectionTitleStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  private void createSectionTitleRow(
      Sheet sheet, int rowIndex, String title, CellStyle sectionTitleStyle) {
    Row row = sheet.createRow(rowIndex);
    Cell cell = row.createCell(0);
    cell.setCellValue(title);
    cell.setCellStyle(sectionTitleStyle);
  }

  private void createLabelValueRow(Sheet sheet, int rowIndex, String label, String value) {
    Row row = sheet.createRow(rowIndex);
    row.createCell(0).setCellValue(label);
    if (value != null) {
      row.createCell(1).setCellValue(value);
    }
  }

  private String formatGuestName(BookingsByActivityDateReportRow row) {
    String firstName = nullToBlank(row.guestFirstName());
    String lastName = nullToBlank(row.guestLastName());
    return (firstName + " " + lastName).trim();
  }

  private String formatActivityTime(String eventTime) {
    return parseEventTime(eventTime).format(ACTIVITY_TIME_FORMAT).toLowerCase(Locale.ENGLISH);
  }

  private double daysToPurchase(BookingsByActivityDateReportRow row) {
    if (row.purchaseDate() == null || row.eventDate() == null) {
      return 0;
    }
    ZonedDateTime purchase = row.purchaseDate().withZoneSameInstant(REPORT_ZONE);
    ZonedDateTime activity =
        LocalDateTime.of(row.eventDate(), parseEventTime(row.eventTime())).atZone(REPORT_ZONE);
    long minutes = Duration.between(purchase, activity).toMinutes();
    return Math.round(minutes / (24.0 * 60.0) * 100.0) / 100.0;
  }

  private BigDecimal allocatedDiscount(BookingsByActivityDateReportRow row) {
    BigDecimal bookingDiscount = nullToZero(row.bookingDiscount());
    BigDecimal totalPaid = nullToZero(row.bookingTotalPaidPrice());
    BigDecimal eventSubtotal = nullToZero(row.eventSubtotal());
    if (bookingDiscount.signum() == 0 || totalPaid.signum() == 0) {
      return BigDecimal.ZERO;
    }
    return bookingDiscount
        .multiply(eventSubtotal)
        .divide(totalPaid, 2, RoundingMode.HALF_UP);
  }

  private static int countPassengers(List<ReportDataRepository.TicketQuantityRow> ticketRows) {
    return ticketRows.stream().mapToInt(ReportDataRepository.TicketQuantityRow::quantity).sum();
  }

  private LocalTime parseEventTime(String eventTime) {
    if (eventTime == null || eventTime.isBlank()) {
      return LocalTime.MIDNIGHT;
    }
    String normalized = eventTime.trim();
    String upperNormalized = normalized.toUpperCase(Locale.ENGLISH);
    for (DateTimeFormatter formatter :
        List.of(
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))) {
      try {
        return LocalTime.parse(upperNormalized, formatter);
      } catch (DateTimeParseException ignored) {
        // try next pattern
      }
    }
    if (normalized.length() >= 5) {
      return LocalTime.parse(normalized.substring(0, 5));
    }
    return LocalTime.parse(normalized);
  }

  private void setTextCell(Row row, int columnIndex, String value) {
    if (value == null || value.isBlank()) {
      row.createCell(columnIndex).setBlank();
      return;
    }
    row.createCell(columnIndex).setCellValue(value);
  }

  private void setMoneyCell(Row row, int columnIndex, BigDecimal value) {
    row.createCell(columnIndex).setCellValue(nullToZero(value).doubleValue());
  }

  private void setDateCell(Row row, int columnIndex, LocalDate value) {
    if (value == null) {
      row.createCell(columnIndex).setBlank();
      return;
    }
    row.createCell(columnIndex).setCellValue(value.format(REPORT_DAY_FORMAT));
  }

  private void setDateCreatedCell(Row row, int columnIndex, ZonedDateTime value) {
    if (value == null) {
      row.createCell(columnIndex).setBlank();
      return;
    }
    row.createCell(columnIndex)
        .setCellValue(
            value.withZoneSameInstant(REPORT_ZONE).toLocalDateTime().format(DATE_CREATED_FORMAT));
  }

  private BigDecimal nullToZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static String nullToBlank(String value) {
    return value == null ? "" : value;
  }

  private static final class ActivityTotals {
    private final List<BigDecimal> daysToPurchaseValues = new ArrayList<>();
    private int totalPassengers = 0;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;

    private void add(
        BigDecimal daysToPurchase,
        List<ReportDataRepository.TicketQuantityRow> ticketRows,
        BigDecimal rowSubtotal,
        BigDecimal rowDiscount,
        BigDecimal rowTotal) {
      daysToPurchaseValues.add(daysToPurchase);
      totalPassengers += countPassengers(ticketRows);
      subtotal = subtotal.add(rowSubtotal);
      discount = discount.add(rowDiscount);
      total = total.add(rowTotal);
    }

    private String averageDaysToPurchase() {
      if (daysToPurchaseValues.isEmpty()) {
        return "0";
      }
      BigDecimal sum = daysToPurchaseValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
      return sum.divide(BigDecimal.valueOf(daysToPurchaseValues.size()), 2, RoundingMode.HALF_UP)
          .stripTrailingZeros()
          .toPlainString();
    }

    private String medianDaysToPurchase() {
      if (daysToPurchaseValues.isEmpty()) {
        return "0";
      }
      List<BigDecimal> sorted = new ArrayList<>(daysToPurchaseValues);
      sorted.sort(Comparator.naturalOrder());
      int middle = sorted.size() / 2;
      if (sorted.size() % 2 == 0) {
        return sorted
            .get(middle - 1)
            .add(sorted.get(middle))
            .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString();
      }
      return sorted.get(middle).setScale(0, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
  }
}
