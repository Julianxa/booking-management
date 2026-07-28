package com.example.report.excel;

import com.example.exception.general.FileOperationException;
import com.example.model.record.RedeemedGiftCertificateCodesReportRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class RedeemedGiftCertificateCodesExcelBuilder {
  private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Hong_Kong");

  private static final DateTimeFormatter REPORT_DATE_FORMAT =
      DateTimeFormatter.ofPattern("EEEE, MMM dd yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter REPORT_GENERATED_ON_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private static final String[] HEADERS = {
    "Type",
    "GC ID",
    "Online Code",
    "Status",
    "Booking No.",
    "Date Issued",
    "Expiry",
    "Date Redeemed",
    "Days To Redeemed",
    "Purchaser Name",
    "Description",
    "Wholesale Value",
    "Retail Value",
    "Redeemed Value",
    "Difference",
    "Sub-Total",
    "Total",
    "Discounts",
    "Net Total"
  };

  public byte[] build(
      LocalDate startDate,
      LocalDate endDate,
      String generatedBy,
      List<RedeemedGiftCertificateCodesReportRow> rows) {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      String sheetName =
          startDate + "-Report-" + UUID.randomUUID().toString().replace("-", "").substring(0, 13);
      Sheet sheet = workbook.createSheet(sheetName.substring(0, Math.min(sheetName.length(), 31)));

      ZonedDateTime generatedAt = ZonedDateTime.now(REPORT_ZONE);
      int rowIndex = writeMetadata(sheet, startDate, endDate, generatedBy, generatedAt);

      writeHeaderRow(sheet, rowIndex++);

      BigDecimal totalWholesale = BigDecimal.ZERO;
      BigDecimal totalRetail = BigDecimal.ZERO;
      BigDecimal totalRedeemed = BigDecimal.ZERO;
      BigDecimal totalDifference = BigDecimal.ZERO;
      BigDecimal totalSubTotal = BigDecimal.ZERO;
      BigDecimal totalTotal = BigDecimal.ZERO;
      BigDecimal totalDiscounts = BigDecimal.ZERO;
      BigDecimal totalNetTotal = BigDecimal.ZERO;
      List<Long> daysToRedeemedValues = new ArrayList<>();
      for (RedeemedGiftCertificateCodesReportRow row : rows) {
        writeDataRow(sheet, rowIndex++, row);
        BigDecimal discount = nullToZero(row.redeemedValue());
        BigDecimal netTotal = netTotalAfterDiscount(row);
        totalWholesale = totalWholesale.add(nullToZero(row.wholesaleValue()));
        totalRetail = totalRetail.add(nullToZero(row.retailValue()));
        totalRedeemed = totalRedeemed.add(discount);
        totalDifference = totalDifference.add(nullToZero(row.difference()));
        totalSubTotal = totalSubTotal.add(nullToZero(row.bookingTotal()));
        totalTotal = totalTotal.add(nullToZero(row.bookingTotal()));
        totalDiscounts = totalDiscounts.add(discount);
        totalNetTotal = totalNetTotal.add(netTotal);
        daysToRedeemedValues.add(row.daysToRedeemed());
      }

      rowIndex =
          writeSummaryRows(
              sheet,
              rowIndex,
              totalWholesale,
              totalRetail,
              totalRedeemed,
              totalDifference,
              totalSubTotal,
              totalTotal,
              totalDiscounts,
              totalNetTotal,
              daysToRedeemedValues);

      for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
        sheet.autoSizeColumn(columnIndex);
      }

      workbook.write(outputStream);
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new FileOperationException("Failed to generate redeemed gift certificate codes report");
    }
  }

  private int writeMetadata(
      Sheet sheet,
      LocalDate startDate,
      LocalDate endDate,
      String generatedBy,
      ZonedDateTime generatedAt) {
    int row = 2;
    createValueRow(sheet, row++, "ALL REDEEMED GIFT CERTIFICATES");
    row++;
    createValueRow(sheet, row++, "Report Start Date:");
    createValueRow(sheet, row++, startDate.format(REPORT_DATE_FORMAT));
    createValueRow(sheet, row++, "Report End Date:");
    createValueRow(sheet, row++, endDate.format(REPORT_DATE_FORMAT));
    createValueRow(sheet, row++, "Report Generated On:");
    createValueRow(
        sheet, row++, generatedAt.withZoneSameInstant(REPORT_ZONE).format(REPORT_GENERATED_ON_FORMAT));
    createValueRow(sheet, row++, "Generated By:");
    createValueRow(sheet, row++, generatedBy != null ? generatedBy : "System");
    row++;
    return row;
  }

  private void writeHeaderRow(Sheet sheet, int rowIndex) {
    Row row = sheet.createRow(rowIndex);
    for (int i = 0; i < HEADERS.length; i++) {
      row.createCell(i).setCellValue(HEADERS[i]);
    }
    ReportExcelStyles.applyGreyToEntireRow(row, HEADERS.length, true);
  }

  private void writeDataRow(Sheet sheet, int rowIndex, RedeemedGiftCertificateCodesReportRow row) {
    Row excelRow = sheet.createRow(rowIndex);
    excelRow.createCell(0).setCellValue(row.typeLabel());
    excelRow.createCell(1).setCellValue(row.giftCertificateRefNo());
    excelRow.createCell(2).setCellValue(row.promoCode());
    excelRow.createCell(3).setCellValue(row.statusLabel());
    if (row.bookingRefNo() != null && !row.bookingRefNo().isBlank()) {
      excelRow.createCell(4).setCellValue(row.bookingRefNo());
    }
    if (row.dateIssued() != null) {
      excelRow.createCell(5).setCellValue(row.dateIssued().format(DAY_FORMAT));
    }
    if (row.expiryDate() != null) {
      excelRow.createCell(6).setCellValue(row.expiryDate().format(DAY_FORMAT));
    }
    if (row.dateRedeemed() != null) {
      excelRow
          .createCell(7)
          .setCellValue(row.dateRedeemed().withZoneSameInstant(REPORT_ZONE).toLocalDate().format(DAY_FORMAT));
    }
    excelRow.createCell(8).setCellValue(row.daysToRedeemed());
    if (row.purchaserName() != null && !row.purchaserName().isBlank()) {
      excelRow.createCell(9).setCellValue(row.purchaserName());
    }
    if (row.description() != null) {
      excelRow.createCell(10).setCellValue(row.description());
    }
    setNumericCell(excelRow.createCell(11), row.wholesaleValue());
    setNumericCell(excelRow.createCell(12), row.retailValue());
    setNumericCell(excelRow.createCell(13), row.redeemedValue());
    setNumericCell(excelRow.createCell(14), row.difference());
    setNumericCell(excelRow.createCell(15), row.bookingTotal());
    setNumericCell(excelRow.createCell(16), row.bookingTotal());
    setNumericCell(excelRow.createCell(17), row.redeemedValue());
    setNumericCell(excelRow.createCell(18), netTotalAfterDiscount(row));
  }

  private int writeSummaryRows(
      Sheet sheet,
      int rowIndex,
      BigDecimal totalWholesale,
      BigDecimal totalRetail,
      BigDecimal totalRedeemed,
      BigDecimal totalDifference,
      BigDecimal totalSubTotal,
      BigDecimal totalTotal,
      BigDecimal totalDiscounts,
      BigDecimal totalNetTotal,
      List<Long> daysToRedeemedValues) {
    Row totalRow = sheet.createRow(rowIndex++);
    setNumericCell(totalRow.createCell(11), totalWholesale);
    setNumericCell(totalRow.createCell(12), totalRetail);
    setNumericCell(totalRow.createCell(13), totalRedeemed);
    setNumericCell(totalRow.createCell(14), totalDifference);
    setNumericCell(totalRow.createCell(15), totalSubTotal);
    setNumericCell(totalRow.createCell(16), totalTotal);
    setNumericCell(totalRow.createCell(17), totalDiscounts);
    setNumericCell(totalRow.createCell(18), totalNetTotal);

    if (!daysToRedeemedValues.isEmpty()) {
      double average =
          daysToRedeemedValues.stream().mapToLong(Long::longValue).average().orElse(0);
      totalRow
          .createCell(8)
          .setCellValue(
              "Avg: "
                  + BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString());
    }
    ReportExcelStyles.applyGreyToEntireRow(totalRow, HEADERS.length, true);

    if (!daysToRedeemedValues.isEmpty()) {
      Row medianRow = sheet.createRow(rowIndex++);
      medianRow.createCell(8).setCellValue("Median: " + median(daysToRedeemedValues));
      ReportExcelStyles.applyGreyToEntireRow(medianRow, HEADERS.length, true);
    }

    return rowIndex;
  }

  private long median(List<Long> values) {
    List<Long> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int size = sorted.size();
    if (size % 2 == 1) {
      return sorted.get(size / 2);
    }
    return Math.round((sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0);
  }

  private void createValueRow(Sheet sheet, int rowIndex, String value) {
    Row row = sheet.createRow(rowIndex);
    row.createCell(0).setCellValue(value);
    ReportExcelStyles.applyGreyToEntireRow(row, HEADERS.length, false);
  }

  private void setNumericCell(Cell cell, BigDecimal value) {
    if (value != null) {
      cell.setCellValue(value.doubleValue());
    }
  }

  private BigDecimal netTotalAfterDiscount(RedeemedGiftCertificateCodesReportRow row) {
    return nullToZero(row.bookingTotal()).subtract(nullToZero(row.redeemedValue())).max(BigDecimal.ZERO);
  }

  private BigDecimal nullToZero(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }
}
