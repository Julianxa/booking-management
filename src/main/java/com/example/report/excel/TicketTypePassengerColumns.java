package com.example.report.excel;

import com.example.repository.ReportDataRepository;
import org.apache.poi.ss.usermodel.Row;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class TicketTypePassengerColumns {
  private static final String PASSENGERS_HEADER = "Passengers";

  private TicketTypePassengerColumns() {}

  static List<String> collectTicketTypeNames(
      Collection<List<ReportDataRepository.TicketQuantityRow>> allTicketRows) {
    LinkedHashSet<String> names = new LinkedHashSet<>();
    if (allTicketRows == null) {
      return List.of();
    }
    for (List<ReportDataRepository.TicketQuantityRow> ticketRows : allTicketRows) {
      if (ticketRows == null) {
        continue;
      }
      for (ReportDataRepository.TicketQuantityRow ticketRow : ticketRows) {
        String name = normalizeTicketTypeName(ticketRow.ticketName());
        if (!name.isBlank()) {
          names.add(name);
        }
      }
    }
    List<String> namesList = new ArrayList<>(names);
    namesList.sort(String.CASE_INSENSITIVE_ORDER);
    return List.copyOf(namesList);
  }

  static List<String> collectTicketTypeNamesFromMap(
      Map<Long, List<ReportDataRepository.TicketQuantityRow>> ticketQuantitiesByBookingEventId) {
    if (ticketQuantitiesByBookingEventId == null || ticketQuantitiesByBookingEventId.isEmpty()) {
      return List.of();
    }
    return collectTicketTypeNames(ticketQuantitiesByBookingEventId.values());
  }

  static String[] expandHeaders(String[] baseHeaders, List<String> ticketTypeNames) {
    List<String> passengerColumns =
        ticketTypeNames == null || ticketTypeNames.isEmpty()
            ? List.of(PASSENGERS_HEADER)
            : ticketTypeNames;

    List<String> headers = new ArrayList<>(baseHeaders.length + Math.max(passengerColumns.size() - 1, 0));
    boolean replaced = false;
    for (String header : baseHeaders) {
      if (PASSENGERS_HEADER.equals(header) && !replaced) {
        headers.addAll(passengerColumns);
        replaced = true;
      } else {
        headers.add(header);
      }
    }
    if (!replaced) {
      headers.addAll(passengerColumns);
    }
    return headers.toArray(String[]::new);
  }

  static Map<String, Integer> quantityByTicketType(
      List<ReportDataRepository.TicketQuantityRow> ticketRows) {
    Map<String, Integer> quantities = new LinkedHashMap<>();
    if (ticketRows == null) {
      return quantities;
    }
    for (ReportDataRepository.TicketQuantityRow ticketRow : ticketRows) {
      String name = normalizeTicketTypeName(ticketRow.ticketName());
      if (name.isBlank()) {
        continue;
      }
      quantities.merge(name, ticketRow.quantity(), Integer::sum);
    }
    return quantities;
  }

  static int writeQuantities(
      Row excelRow,
      int startColumn,
      List<String> ticketTypeNames,
      List<ReportDataRepository.TicketQuantityRow> ticketRows) {
    Map<String, Integer> quantities = quantityByTicketType(ticketRows);
    if (ticketTypeNames == null || ticketTypeNames.isEmpty()) {
      excelRow
          .createCell(startColumn++)
          .setCellValue(quantities.values().stream().mapToInt(Integer::intValue).sum());
      return startColumn;
    }
    for (String ticketTypeName : ticketTypeNames) {
      excelRow
          .createCell(startColumn++)
          .setCellValue(quantities.getOrDefault(ticketTypeName, 0));
    }
    return startColumn;
  }

  static void addToTotals(
      Map<String, Integer> totalsByTicketType,
      List<String> ticketTypeNames,
      List<ReportDataRepository.TicketQuantityRow> ticketRows) {
    Map<String, Integer> quantities = quantityByTicketType(ticketRows);
    if (ticketTypeNames == null || ticketTypeNames.isEmpty()) {
      totalsByTicketType.merge(
          PASSENGERS_HEADER,
          quantities.values().stream().mapToInt(Integer::intValue).sum(),
          Integer::sum);
      return;
    }
    for (String ticketTypeName : ticketTypeNames) {
      totalsByTicketType.merge(
          ticketTypeName, quantities.getOrDefault(ticketTypeName, 0), Integer::sum);
    }
  }

  static int writeTotals(
      Row excelRow, int startColumn, List<String> ticketTypeNames, Map<String, Integer> totalsByTicketType) {
    if (ticketTypeNames == null || ticketTypeNames.isEmpty()) {
      excelRow
          .createCell(startColumn++)
          .setCellValue(totalsByTicketType.getOrDefault(PASSENGERS_HEADER, 0));
      return startColumn;
    }
    for (String ticketTypeName : ticketTypeNames) {
      excelRow
          .createCell(startColumn++)
          .setCellValue(totalsByTicketType.getOrDefault(ticketTypeName, 0));
    }
    return startColumn;
  }

  private static String normalizeTicketTypeName(String ticketName) {
    return ticketName == null ? "" : ticketName.trim();
  }
}
