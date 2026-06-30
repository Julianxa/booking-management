package com.example.repository;

import com.example.model.record.BookingsByActivityDateReportRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReportDataRepository {
  private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Hong_Kong");

  private static final String EXCLUDED_BOOKING_STATUSES =
      "'CANCELLED', 'FAILED', 'EXPIRED', 'REFUNDED'";

  @PersistenceContext private EntityManager entityManager;

  public long countBookingEventsInDateRange(LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT COUNT(*)
        FROM booking_events be
        WHERE be.event_date BETWEEN :startDate AND :endDate
        """;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);
    return toLong(query.getSingleResult());
  }

  @SuppressWarnings("unchecked")
  public List<BookingsByActivityDateReportRow> findBookingsByActivityDate(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT
            b.ref_no,
            be.id,
            be.event_date,
            be.event_time,
            be.total,
            b.created_at,
            b.discount,
            b.total_paid_price,
            b.final_paid_amount,
            b.gift_certificate_id,
            e.type,
            e.name,
            e.name_zh_hk,
            e.category,
            ba.first_name,
            ba.last_name,
            ba.country,
            u.first_name,
            u.last_name,
            u.role,
            o.name,
            o.company_type,
            o.company_group
        FROM booking_events be
        INNER JOIN bookings b ON b.id = be.booking_id
        INNER JOIN events e ON e.id = be.event_id
        LEFT JOIN booking_attendees ba ON ba.id = (
            SELECT ba2.id
            FROM booking_attendees ba2
            WHERE ba2.booking_event_id = be.id
            ORDER BY ba2.sequence ASC, ba2.id ASC
            LIMIT 1
        )
        LEFT JOIN users u ON u.id = b.user_id
        LEFT JOIN organizations o ON o.id = u.org_id AND u.role = 'AGENT'
        WHERE be.event_date BETWEEN :startDate AND :endDate
          AND be.cancelled_at IS NULL
          AND be.status <> 'CANCELLED'
          AND b.status NOT IN ("""
            + EXCLUDED_BOOKING_STATUSES
            + """
          )
        ORDER BY be.event_date ASC, be.event_time ASC, b.id ASC
        """;

    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);

    return ((List<Object[]>) query.getResultList()).stream().map(this::mapRow).toList();
  }

  @SuppressWarnings("unchecked")
  public Map<Long, List<TicketQuantityRow>> findTicketQuantitiesByBookingEventIds(
      List<Long> bookingEventIds) {
    if (bookingEventIds == null || bookingEventIds.isEmpty()) {
      return Map.of();
    }

    String sql =
        """
        SELECT bi.booking_event_id, tt.name, tt.name_zh_hk, tt.name_zh_cn, bi.quantity
        FROM booking_items bi
        INNER JOIN ticket_types tt ON tt.id = bi.ticket_type_id
        WHERE bi.booking_event_id IN (:bookingEventIds)
        """;

    Query query = entityManager.createNativeQuery(sql);
    query.setParameter("bookingEventIds", bookingEventIds);

    Map<Long, List<TicketQuantityRow>> result = new HashMap<>();
    for (Object[] row : (List<Object[]>) query.getResultList()) {
      Long bookingEventId = toLongObject(row[0]);
      String ticketName = row[1] != null ? row[1].toString() : "";
      String ticketNameZhHk = row[2] != null ? row[2].toString() : "";
      String ticketNameZhCn = row[3] != null ? row[3].toString() : "";
      int quantity = toInt(row[4]);
      result
          .computeIfAbsent(bookingEventId, ignored -> new java.util.ArrayList<>())
          .add(new TicketQuantityRow(ticketName, ticketNameZhHk, ticketNameZhCn, quantity));
    }
    return result;
  }

  private void bindDateRange(Query query, LocalDate startDate, LocalDate endDate) {
    query.setParameter("startDate", Date.valueOf(startDate));
    query.setParameter("endDate", Date.valueOf(endDate));
  }

  private BookingsByActivityDateReportRow mapRow(Object[] row) {
    return new BookingsByActivityDateReportRow(
        asString(row[0]),
        asLong(row[1]),
        asLocalDate(row[2]),
        asString(row[3]),
        asBigDecimal(row[4]),
        asZonedDateTime(row[5]),
        asBigDecimal(row[6]),
        asBigDecimal(row[7]),
        asBigDecimal(row[8]),
        asLong(row[9]),
        asString(row[10]),
        asString(row[11]),
        asString(row[12]),
        asString(row[13]),
        asString(row[14]),
        asString(row[15]),
        asString(row[16]),
        asString(row[17]),
        asString(row[18]),
        asString(row[19]),
        asString(row[20]),
        asString(row[21]),
        asString(row[22]));
  }

  private Long asLong(Object value) {
    if (value == null) {
      return null;
    }
    return toLongObject(value);
  }

  private long toLong(Object value) {
    if (value == null) {
      return 0L;
    }
    return toLongObject(value);
  }

  private Long toLongObject(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String string && !string.isBlank()) {
      return Long.parseLong(string.trim());
    }
    throw new IllegalArgumentException("Cannot convert value to long: " + value);
  }

  private int toInt(Object value) {
    if (value == null) {
      return 0;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String string && !string.isBlank()) {
      return Integer.parseInt(string.trim());
    }
    throw new IllegalArgumentException("Cannot convert value to int: " + value);
  }

  private String asString(Object value) {
    return value == null ? null : value.toString();
  }

  private BigDecimal asBigDecimal(Object value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    return new BigDecimal(value.toString());
  }

  private LocalDate asLocalDate(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDate localDate) {
      return localDate;
    }
    if (value instanceof Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    if (value instanceof java.util.Date utilDate) {
      return new Date(utilDate.getTime()).toLocalDate();
    }
    return LocalDate.parse(value.toString());
  }

  private ZonedDateTime asZonedDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof ZonedDateTime zonedDateTime) {
      return zonedDateTime;
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.atZone(REPORT_ZONE);
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant().atZone(REPORT_ZONE);
    }
    return ZonedDateTime.parse(value.toString());
  }

  public record TicketQuantityRow(
      String ticketName, String ticketNameZhHk, String ticketNameZhCn, int quantity) {}
}
