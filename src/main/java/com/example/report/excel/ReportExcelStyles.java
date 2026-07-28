package com.example.report.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

final class ReportExcelStyles {
  private ReportExcelStyles() {}

  static CellStyle createGreyStyle(Workbook workbook, boolean bold) {
    CellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    Font font = workbook.createFont();
    font.setBold(bold);
    style.setFont(font);
    return style;
  }

  /** Fills every cell in the row (creating blanks as needed) with a grey background. */
  static void applyGreyToEntireRow(Row row, int columnCount, boolean bold) {
    CellStyle style = createGreyStyle(row.getSheet().getWorkbook(), bold);
    for (int i = 0; i < columnCount; i++) {
      Cell cell = row.getCell(i);
      if (cell == null) {
        cell = row.createCell(i);
      }
      cell.setCellStyle(style);
    }
  }
}
