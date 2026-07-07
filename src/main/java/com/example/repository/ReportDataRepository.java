package com.example.repository;

import com.example.model.record.BookingsByActivityDateReportRow;
import com.example.model.record.CountryOfOriginReportRow;
import com.example.model.record.ExpiredGiftCertificateCodesReportRow;
import com.example.model.record.GiftCertificateUsedInBookingReportRow;
import com.example.model.record.RedeemedGiftCertificateCodesReportRow;
import com.example.model.record.UnredeemedGiftCertificateCodesReportRow;
import com.example.model.record.PromoCodesByTransactionDateReportRow;
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

  private static final String PROMO_GIFT_CERTIFICATE_TYPES =
      "'VALUE', 'EVENT', 'PERSONAL_VALUE', 'PERSONAL_EVENT'";

  private static final String TRANSACTION_DATE_TIME_SQL =
      "CASE WHEN b.type = 'OFFLINE_PAYMENT' THEN b.created_at ELSE p.paid_at END";

  private static final String TRANSACTION_DATE_SQL =
      "DATE(" + TRANSACTION_DATE_TIME_SQL + ")";

  private static final String TRANSACTION_DATE_BETWEEN_SQL =
      "WHERE " + TRANSACTION_DATE_SQL + " BETWEEN :startDate AND :endDate";

  private static final String TRANSACTION_DATE_ELIGIBILITY_SQL =
      "(b.type = 'OFFLINE_PAYMENT' OR (b.type = 'ONLINE_PAYMENT' AND p.paid_at IS NOT NULL))";

  private static final String PROMO_CODES_TRANSACTION_DATE_WHERE_SQL =
      TRANSACTION_DATE_BETWEEN_SQL
          + " AND "
          + TRANSACTION_DATE_ELIGIBILITY_SQL
          + " AND gc.type IN ("
          + PROMO_GIFT_CERTIFICATE_TYPES
          + ")";

  private static final String ORDER_BY_TRANSACTION_DATE_TIME_SQL =
      "ORDER BY " + TRANSACTION_DATE_TIME_SQL + " ASC";

  private static final String PROMO_CODES_ORDER_BY_SQL =
      ORDER_BY_TRANSACTION_DATE_TIME_SQL + ", be.event_date ASC, be.event_time ASC, b.id ASC";

  private static final String ONLINE_PURCHASE_DATE_WHERE_SQL =
      TRANSACTION_DATE_BETWEEN_SQL
          + " AND b.type = 'ONLINE_PAYMENT'"
          + " AND p.paid_at IS NOT NULL";

  private static final String BOOKINGS_BY_PURCHASE_DATE_WHERE_SQL =
      ONLINE_PURCHASE_DATE_WHERE_SQL
          + " AND be.cancelled_at IS NULL"
          + " AND be.status <> 'CANCELLED'"
          + " AND b.status NOT IN ("
          + EXCLUDED_BOOKING_STATUSES
          + ")";

  private static final String BOOKINGS_BY_PURCHASE_DATE_ORDER_BY_SQL =
      ORDER_BY_TRANSACTION_DATE_TIME_SQL + ", be.event_date ASC, be.event_time ASC, b.id ASC";

  private static final String ACTIVITY_DATE_BOOKING_FILTER_SQL =
      "be.event_date BETWEEN :startDate AND :endDate"
          + " AND be.cancelled_at IS NULL"
          + " AND be.status <> 'CANCELLED'"
          + " AND b.status NOT IN ("
          + EXCLUDED_BOOKING_STATUSES
          + ")";

  private static final String ACTIVITY_DATE_BOOKING_WHERE_SQL =
      "WHERE " + ACTIVITY_DATE_BOOKING_FILTER_SQL;

  private static final String ACTIVITY_DATE_COUNTRY_OF_ORIGIN_WHERE_SQL =
      ACTIVITY_DATE_BOOKING_WHERE_SQL
          + " AND ba.country IS NOT NULL AND TRIM(ba.country) <> ''";

  private static final String EXPIRED_GIFT_CERTIFICATE_EXPIRY_WHERE_SQL =
      "WHERE gc.expiry_date BETWEEN :startDate AND :endDate"
          + " AND gc.cancelled_at IS NULL";

  private static final String EXPIRED_GIFT_CERTIFICATE_ORDER_BY_SQL =
      "ORDER BY gc.expiry_date ASC, gc.id ASC";

  private static final String REDEEMED_GIFT_CERTIFICATE_REDEMPTION_WHERE_SQL =
      "WHERE gcr.status = 'SUCCESS'"
          + " AND DATE(gcr.redeemed_at) BETWEEN :startDate AND :endDate";

  private static final String REDEEMED_GIFT_CERTIFICATE_ORDER_BY_SQL =
      "ORDER BY gcr.redeemed_at ASC, gc.id ASC";

  private static final String UNREDEEMED_GIFT_CERTIFICATE_WHERE_SQL =
      "WHERE gc.cancelled_at IS NULL"
          + " AND gc.remaining_quantity > 0"
          + " AND NOT EXISTS ("
          + "   SELECT 1 FROM gift_certificate_redemptions gcr"
          + "   WHERE gcr.gift_certificate_id = gc.id AND gcr.status = 'SUCCESS'"
          + " )"
          + " AND DATE(gc.created_at) BETWEEN :startDate AND :endDate";

  private static final String UNREDEEMED_GIFT_CERTIFICATE_ORDER_BY_SQL =
      "ORDER BY gc.created_at ASC, gc.id ASC";

  private static final String GIFT_CERTIFICATE_TYPE_LABEL_SQL =
      "CASE gc.type"
          + " WHEN 'VALUE' THEN 'OPEN'"
          + " WHEN 'PERSONAL_VALUE' THEN 'UNIQUE_CODE_OPEN'"
          + " WHEN 'PERSONAL_EVENT' THEN 'UNIQUE_CODE_EVENT'"
          + " WHEN 'EVENT' THEN 'EVENT'"
          + " ELSE gc.type"
          + " END";

  private static final String BOOKINGS_BY_ACTIVITY_DATE_ORDER_BY_SQL =
      "ORDER BY be.event_date ASC, be.event_time ASC, b.id ASC";

  private static final String BOOKINGS_BY_ACTIVITY_DATE_SELECT_SQL =
      """
        SELECT
            b.ref_no,
            b.id,
            b.type,
            be.id,
            be.event_date,
            be.event_time,
            be.total,
            """
          + TRANSACTION_DATE_TIME_SQL
          + """
            ,
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
        LEFT JOIN payments p ON p.booking_id = b.id AND p.payment_status = 'SUCCEEDED'
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
        """;

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

  public long countDistinctBookingsWithCountryByActivityDateInRange(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT COUNT(DISTINCT b.id)
        FROM booking_attendees ba
        INNER JOIN booking_events be ON be.id = ba.booking_event_id
        INNER JOIN bookings b ON b.id = be.booking_id
        """
            + ACTIVITY_DATE_COUNTRY_OF_ORIGIN_WHERE_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);
    return toLong(query.getSingleResult());
  }

  @SuppressWarnings("unchecked")
  public List<CountryOfOriginReportRow> findCountryOfOriginByActivityDate(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT
            UPPER(TRIM(ba.country)) AS country_code,
            COUNT(ba.id) AS total_passengers,
            COUNT(DISTINCT b.id) AS booking_count
        FROM booking_attendees ba
        INNER JOIN booking_events be ON be.id = ba.booking_event_id
        INNER JOIN bookings b ON b.id = be.booking_id
        """
            + ACTIVITY_DATE_COUNTRY_OF_ORIGIN_WHERE_SQL
            + """
        GROUP BY UPPER(TRIM(ba.country))
        ORDER BY total_passengers DESC, country_code ASC
        """;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);

    return ((List<Object[]>) query.getResultList())
        .stream().map(this::mapCountryOfOriginRow).toList();
  }

  public long countExpiredGiftCertificateCodesInRange(LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT COUNT(*)
        FROM gift_certificates gc
        """
            + EXPIRED_GIFT_CERTIFICATE_EXPIRY_WHERE_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);
    return toLong(query.getSingleResult());
  }

  @SuppressWarnings("unchecked")
  public List<ExpiredGiftCertificateCodesReportRow> findExpiredGiftCertificateCodes(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT
            """
            + GIFT_CERTIFICATE_TYPE_LABEL_SQL
            + """
             AS type_label,
            gc.ref_no,
            gc.promo_code,
            CASE WHEN gc.expiry_date < CURRENT_DATE THEN 'Expired' ELSE 'Active' END AS status_label,
            CASE
                WHEN gc.type IN ('VALUE', 'PERSONAL_VALUE') THEN
                    COALESCE(gci.value, 0) + COALESCE(redeemed.total_discount, 0)
                ELSE COALESCE(event_value.event_face_value, 0)
            END AS wholesale_value,
            CASE
                WHEN gc.type IN ('VALUE', 'PERSONAL_VALUE') THEN
                    COALESCE(gci.value, 0) + COALESCE(redeemed.total_discount, 0)
                ELSE COALESCE(event_value.event_face_value, 0)
            END AS retail_value,
            gc.expiry_date,
            NULLIF(
                TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))),
                ''
            ) AS purchaser_name,
            gc.message_to_recipient,
            CASE
                WHEN gc.type IN ('VALUE', 'PERSONAL_VALUE') THEN COALESCE(gci.value, 0)
                WHEN gc.quantity IS NULL OR gc.quantity = 0 THEN 0
                ELSE COALESCE(event_value.event_face_value, 0) * gc.remaining_quantity / gc.quantity
            END AS balance
        FROM gift_certificates gc
        LEFT JOIN users u ON u.id = gc.user_id
        LEFT JOIN (
            SELECT gift_certificate_id, MIN(id) AS item_id
            FROM gift_certificate_items
            GROUP BY gift_certificate_id
        ) gci_pick ON gci_pick.gift_certificate_id = gc.id
        LEFT JOIN gift_certificate_items gci ON gci.id = gci_pick.item_id
        LEFT JOIN (
            SELECT gcr.gift_certificate_id,
                   SUM(COALESCE(b.discount, 0)) AS total_discount
            FROM gift_certificate_redemptions gcr
            INNER JOIN bookings b ON b.id = gcr.booking_id
            WHERE gcr.status = 'SUCCESS'
            GROUP BY gcr.gift_certificate_id
        ) redeemed ON redeemed.gift_certificate_id = gc.id
        LEFT JOIN (
            SELECT gci2.gift_certificate_id,
                   SUM(COALESCE(price_pick.price, 0) * gci2.quantity) AS event_face_value
            FROM gift_certificate_items gci2
            LEFT JOIN (
                SELECT tpp.ticket_type_id, tpp.price
                FROM ticket_price_periods tpp
                INNER JOIN (
                    SELECT ticket_type_id, MAX(id) AS period_id
                    FROM ticket_price_periods
                    GROUP BY ticket_type_id
                ) latest_period ON latest_period.period_id = tpp.id
            ) price_pick ON price_pick.ticket_type_id = gci2.ticket_type_id
            GROUP BY gci2.gift_certificate_id
        ) event_value ON event_value.gift_certificate_id = gc.id
        """
            + EXPIRED_GIFT_CERTIFICATE_EXPIRY_WHERE_SQL
            + " "
            + EXPIRED_GIFT_CERTIFICATE_ORDER_BY_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);

    return ((List<Object[]>) query.getResultList())
        .stream().map(this::mapExpiredGiftCertificateCodesRow).toList();
  }

  public long countRedeemedGiftCertificateCodesInRange(LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT COUNT(*)
        FROM gift_certificate_redemptions gcr
        """
            + REDEEMED_GIFT_CERTIFICATE_REDEMPTION_WHERE_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);
    return toLong(query.getSingleResult());
  }

  @SuppressWarnings("unchecked")
  public List<RedeemedGiftCertificateCodesReportRow> findRedeemedGiftCertificateCodes(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT
            """
            + GIFT_CERTIFICATE_TYPE_LABEL_SQL
            + """
             AS type_label,
            gc.ref_no,
            gc.promo_code,
            'Redeemed' AS status_label,
            b.ref_no,
            DATE(gc.created_at),
            gc.expiry_date,
            gcr.redeemed_at,
            DATEDIFF(gc.expiry_date, DATE(gcr.redeemed_at)),
            NULLIF(
                TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))),
                ''
            ) AS purchaser_name,
            gc.message_to_recipient,
            CASE
                WHEN gc.type IN ('VALUE', 'PERSONAL_VALUE') THEN
                    COALESCE(gci.value, 0) + COALESCE(redeemed.total_discount, 0)
                ELSE COALESCE(event_value.event_face_value, 0)
            END AS wholesale_value,
            CASE
                WHEN gc.type IN ('VALUE', 'PERSONAL_VALUE') THEN
                    COALESCE(gci.value, 0) + COALESCE(redeemed.total_discount, 0)
                ELSE COALESCE(event_value.event_face_value, 0)
            END AS retail_value,
            COALESCE(b.discount, 0),
            CASE
                WHEN gc.type IN ('VALUE', 'PERSONAL_VALUE') THEN
                    COALESCE(gci.value, 0) + COALESCE(redeemed.total_discount, 0)
                ELSE COALESCE(event_value.event_face_value, 0)
            END - COALESCE(b.discount, 0),
            b.total_paid_price
        FROM gift_certificate_redemptions gcr
        INNER JOIN gift_certificates gc ON gc.id = gcr.gift_certificate_id
        INNER JOIN bookings b ON b.id = gcr.booking_id
        LEFT JOIN users u ON u.id = gc.user_id
        LEFT JOIN (
            SELECT gift_certificate_id, MIN(id) AS item_id
            FROM gift_certificate_items
            GROUP BY gift_certificate_id
        ) gci_pick ON gci_pick.gift_certificate_id = gc.id
        LEFT JOIN gift_certificate_items gci ON gci.id = gci_pick.item_id
        LEFT JOIN (
            SELECT gcr_inner.gift_certificate_id,
                   SUM(COALESCE(b_inner.discount, 0)) AS total_discount
            FROM gift_certificate_redemptions gcr_inner
            INNER JOIN bookings b_inner ON b_inner.id = gcr_inner.booking_id
            WHERE gcr_inner.status = 'SUCCESS'
            GROUP BY gcr_inner.gift_certificate_id
        ) redeemed ON redeemed.gift_certificate_id = gc.id
        LEFT JOIN (
            SELECT gci2.gift_certificate_id,
                   SUM(COALESCE(price_pick.price, 0) * gci2.quantity) AS event_face_value
            FROM gift_certificate_items gci2
            LEFT JOIN (
                SELECT tpp.ticket_type_id, tpp.price
                FROM ticket_price_periods tpp
                INNER JOIN (
                    SELECT ticket_type_id, MAX(id) AS period_id
                    FROM ticket_price_periods
                    GROUP BY ticket_type_id
                ) latest_period ON latest_period.period_id = tpp.id
            ) price_pick ON price_pick.ticket_type_id = gci2.ticket_type_id
            GROUP BY gci2.gift_certificate_id
        ) event_value ON event_value.gift_certificate_id = gc.id
        """
            + REDEEMED_GIFT_CERTIFICATE_REDEMPTION_WHERE_SQL
            + " "
            + REDEEMED_GIFT_CERTIFICATE_ORDER_BY_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);

    return ((List<Object[]>) query.getResultList())
        .stream().map(this::mapRedeemedGiftCertificateCodesRow).toList();
  }

  public long countUnredeemedGiftCertificateCodesInRange(LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT COUNT(*)
        FROM gift_certificates gc
        """
            + UNREDEEMED_GIFT_CERTIFICATE_WHERE_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);
    return toLong(query.getSingleResult());
  }

  @SuppressWarnings("unchecked")
  public List<UnredeemedGiftCertificateCodesReportRow> findUnredeemedGiftCertificateCodes(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT
            """
            + GIFT_CERTIFICATE_TYPE_LABEL_SQL
            + """
             AS type_label,
            gc.ref_no,
            gc.promo_code,
            CASE WHEN gc.expiry_date < CURRENT_DATE THEN 'Expired' ELSE 'Active' END AS status_label,
            CASE
                WHEN gc.type IN ('VALUE', 'PERSONAL_VALUE') THEN
                    COALESCE(gci.value, 0) + COALESCE(redeemed.total_discount, 0)
                ELSE COALESCE(event_value.event_face_value, 0)
            END AS wholesale_value,
            CASE
                WHEN gc.type IN ('VALUE', 'PERSONAL_VALUE') THEN
                    COALESCE(gci.value, 0) + COALESCE(redeemed.total_discount, 0)
                ELSE COALESCE(event_value.event_face_value, 0)
            END AS retail_value,
            gc.created_at,
            gc.expiry_date,
            NULLIF(
                TRIM(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, ''))),
                ''
            ) AS purchaser_name,
            gc.message_to_recipient,
            CASE
                WHEN gc.type IN ('VALUE', 'PERSONAL_VALUE') THEN COALESCE(gci.value, 0)
                WHEN gc.quantity IS NULL OR gc.quantity = 0 THEN 0
                ELSE COALESCE(event_value.event_face_value, 0) * gc.remaining_quantity / gc.quantity
            END AS balance
        FROM gift_certificates gc
        LEFT JOIN users u ON u.id = gc.user_id
        LEFT JOIN (
            SELECT gift_certificate_id, MIN(id) AS item_id
            FROM gift_certificate_items
            GROUP BY gift_certificate_id
        ) gci_pick ON gci_pick.gift_certificate_id = gc.id
        LEFT JOIN gift_certificate_items gci ON gci.id = gci_pick.item_id
        LEFT JOIN (
            SELECT gcr.gift_certificate_id,
                   SUM(COALESCE(b.discount, 0)) AS total_discount
            FROM gift_certificate_redemptions gcr
            INNER JOIN bookings b ON b.id = gcr.booking_id
            WHERE gcr.status = 'SUCCESS'
            GROUP BY gcr.gift_certificate_id
        ) redeemed ON redeemed.gift_certificate_id = gc.id
        LEFT JOIN (
            SELECT gci2.gift_certificate_id,
                   SUM(COALESCE(price_pick.price, 0) * gci2.quantity) AS event_face_value
            FROM gift_certificate_items gci2
            LEFT JOIN (
                SELECT tpp.ticket_type_id, tpp.price
                FROM ticket_price_periods tpp
                INNER JOIN (
                    SELECT ticket_type_id, MAX(id) AS period_id
                    FROM ticket_price_periods
                    GROUP BY ticket_type_id
                ) latest_period ON latest_period.period_id = tpp.id
            ) price_pick ON price_pick.ticket_type_id = gci2.ticket_type_id
            GROUP BY gci2.gift_certificate_id
        ) event_value ON event_value.gift_certificate_id = gc.id
        """
            + UNREDEEMED_GIFT_CERTIFICATE_WHERE_SQL
            + " "
            + UNREDEEMED_GIFT_CERTIFICATE_ORDER_BY_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);

    return ((List<Object[]>) query.getResultList())
        .stream().map(this::mapUnredeemedGiftCertificateCodesRow).toList();
  }

  public long countBookingEventsByPurchaseDateInRange(LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT COUNT(*)
        FROM booking_events be
        INNER JOIN bookings b ON b.id = be.booking_id
        LEFT JOIN payments p ON p.booking_id = b.id AND p.payment_status = 'SUCCEEDED'
        """
            + ONLINE_PURCHASE_DATE_WHERE_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);
    return toLong(query.getSingleResult());
  }

  @SuppressWarnings("unchecked")
  public List<BookingsByActivityDateReportRow> findBookingsByPurchaseDate(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        BOOKINGS_BY_ACTIVITY_DATE_SELECT_SQL
            + BOOKINGS_BY_PURCHASE_DATE_WHERE_SQL
            + " "
            + BOOKINGS_BY_PURCHASE_DATE_ORDER_BY_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);

    return ((List<Object[]>) query.getResultList()).stream().map(this::mapRow).toList();
  }

  @SuppressWarnings("unchecked")
  public List<GiftCertificateUsedInBookingReportRow> findGiftCertificatesUsedInBookingsByPurchaseDate(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT
            b.ref_no,
            """
            + TRANSACTION_DATE_TIME_SQL
            + """
            ,
            gc.ref_no,
            COALESCE(
                NULLIF(TRIM(CONCAT(COALESCE(gc_user.first_name, ''), ' ', COALESCE(gc_user.last_name, ''))), ''),
                NULLIF(TRIM(CONCAT(COALESCE(ba.first_name, ''), ' ', COALESCE(ba.last_name, ''))), '')
            ),
            gc.message_to_recipient,
            b.discount
        FROM bookings b
        INNER JOIN gift_certificate_redemptions gcr
            ON gcr.booking_id = b.id AND gcr.status = 'SUCCESS'
        INNER JOIN gift_certificates gc ON gc.id = gcr.gift_certificate_id
        LEFT JOIN payments p ON p.booking_id = b.id AND p.payment_status = 'SUCCEEDED'
        LEFT JOIN users gc_user ON gc_user.id = gc.user_id
        LEFT JOIN booking_attendees ba ON ba.id = (
            SELECT ba2.id
            FROM booking_attendees ba2
            INNER JOIN booking_events be2 ON be2.id = ba2.booking_event_id
            WHERE be2.booking_id = b.id
            ORDER BY ba2.sequence ASC, ba2.id ASC
            LIMIT 1
        )
        """
            + ONLINE_PURCHASE_DATE_WHERE_SQL
            + " AND b.status NOT IN ("
            + EXCLUDED_BOOKING_STATUSES
            + ") "
            + ORDER_BY_TRANSACTION_DATE_TIME_SQL
            + ", b.ref_no ASC";

    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);

    return ((List<Object[]>) query.getResultList())
        .stream().map(this::mapGiftCertificateUsedInBookingRow).toList();
  }

  @SuppressWarnings("unchecked")
  public List<BookingsByActivityDateReportRow> findBookingsByActivityDate(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        BOOKINGS_BY_ACTIVITY_DATE_SELECT_SQL
            + " "
            + ACTIVITY_DATE_BOOKING_WHERE_SQL
            + " "
            + BOOKINGS_BY_ACTIVITY_DATE_ORDER_BY_SQL;

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

  public long countPromoCodesByTransactionDateInRange(LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT COUNT(DISTINCT be.id)
        FROM bookings b
        INNER JOIN gift_certificates gc ON gc.id = b.gift_certificate_id
        LEFT JOIN payments p ON p.booking_id = b.id AND p.payment_status = 'SUCCEEDED'
        INNER JOIN booking_events be ON be.booking_id = b.id
        """
            + PROMO_CODES_TRANSACTION_DATE_WHERE_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);
    return toLong(query.getSingleResult());
  }

  @SuppressWarnings("unchecked")
  public List<PromoCodesByTransactionDateReportRow> findPromoCodesByTransactionDate(
      LocalDate startDate, LocalDate endDate) {
    String sql =
        """
        SELECT
            b.ref_no,
            be.id,
            """
            + TRANSACTION_DATE_TIME_SQL
            + """
            ,
            be.event_date,
            be.event_time,
            e.name,
            e.name_zh_hk,
            ba.first_name,
            ba.last_name,
            be.total,
            gc.promo_code,
            gc.type,
            promo_item.value,
            b.discount,
            b.total_paid_price
        FROM bookings b
        INNER JOIN gift_certificates gc ON gc.id = b.gift_certificate_id
        LEFT JOIN payments p ON p.booking_id = b.id AND p.payment_status = 'SUCCEEDED'
        INNER JOIN gift_certificate_redemptions gcr
            ON gcr.booking_id = b.id AND gcr.status = 'SUCCESS'
        INNER JOIN booking_events be ON be.booking_id = b.id
        INNER JOIN events e ON e.id = be.event_id
        LEFT JOIN (
            SELECT gift_certificate_id, MIN(id) AS item_id
            FROM gift_certificate_items
            GROUP BY gift_certificate_id
        ) promo_item_pick ON promo_item_pick.gift_certificate_id = gc.id
        LEFT JOIN gift_certificate_items promo_item ON promo_item.id = promo_item_pick.item_id
        LEFT JOIN booking_attendees ba ON ba.id = (
            SELECT ba2.id
            FROM booking_attendees ba2
            WHERE ba2.booking_event_id = be.id
            ORDER BY ba2.sequence ASC, ba2.id ASC
            LIMIT 1
        )
        """
            + PROMO_CODES_TRANSACTION_DATE_WHERE_SQL
            + """
          AND be.cancelled_at IS NULL
          AND be.status <> 'CANCELLED'
          AND b.status NOT IN ("""
            + EXCLUDED_BOOKING_STATUSES
            + """
          )
        """
            + PROMO_CODES_ORDER_BY_SQL;
    Query query = entityManager.createNativeQuery(sql);
    bindDateRange(query, startDate, endDate);

    return ((List<Object[]>) query.getResultList())
        .stream().map(this::mapPromoCodesByTransactionDateRow).toList();
  }

  private GiftCertificateUsedInBookingReportRow mapGiftCertificateUsedInBookingRow(Object[] row) {
    return new GiftCertificateUsedInBookingReportRow(
        asString(row[0]),
        asZonedDateTime(row[1]),
        asString(row[2]),
        asString(row[3]),
        asString(row[4]),
        asBigDecimal(row[5]));
  }

  private CountryOfOriginReportRow mapCountryOfOriginRow(Object[] row) {
    return new CountryOfOriginReportRow(
        asString(row[0]), toLong(row[1]), toLong(row[2]));
  }

  private ExpiredGiftCertificateCodesReportRow mapExpiredGiftCertificateCodesRow(Object[] row) {
    return new ExpiredGiftCertificateCodesReportRow(
        asString(row[0]),
        asString(row[1]),
        asString(row[2]),
        asString(row[3]),
        asBigDecimal(row[4]),
        asBigDecimal(row[5]),
        asLocalDate(row[6]),
        asString(row[7]),
        asString(row[8]),
        asBigDecimal(row[9]));
  }

  private RedeemedGiftCertificateCodesReportRow mapRedeemedGiftCertificateCodesRow(Object[] row) {
    return new RedeemedGiftCertificateCodesReportRow(
        asString(row[0]),
        asString(row[1]),
        asString(row[2]),
        asString(row[3]),
        asString(row[4]),
        asLocalDate(row[5]),
        asLocalDate(row[6]),
        asZonedDateTime(row[7]),
        toLong(row[8]),
        asString(row[9]),
        asString(row[10]),
        asBigDecimal(row[11]),
        asBigDecimal(row[12]),
        asBigDecimal(row[13]),
        asBigDecimal(row[14]),
        asBigDecimal(row[15]));
  }

  private UnredeemedGiftCertificateCodesReportRow mapUnredeemedGiftCertificateCodesRow(Object[] row) {
    return new UnredeemedGiftCertificateCodesReportRow(
        asString(row[0]),
        asString(row[1]),
        asString(row[2]),
        asString(row[3]),
        asBigDecimal(row[4]),
        asBigDecimal(row[5]),
        asZonedDateTime(row[6]),
        asLocalDate(row[7]),
        asString(row[8]),
        asString(row[9]),
        asBigDecimal(row[10]));
  }

  private PromoCodesByTransactionDateReportRow mapPromoCodesByTransactionDateRow(Object[] row) {
    return new PromoCodesByTransactionDateReportRow(
        asString(row[0]),
        asLong(row[1]),
        asZonedDateTime(row[2]),
        asLocalDate(row[3]),
        asString(row[4]),
        asString(row[5]),
        asString(row[6]),
        asString(row[7]),
        asString(row[8]),
        asBigDecimal(row[9]),
        asString(row[10]),
        asString(row[11]),
        asBigDecimal(row[12]),
        asBigDecimal(row[13]),
        asBigDecimal(row[14]));
  }

  private void bindDateRange(Query query, LocalDate startDate, LocalDate endDate) {
    query.setParameter("startDate", Date.valueOf(startDate));
    query.setParameter("endDate", Date.valueOf(endDate));
  }

  private BookingsByActivityDateReportRow mapRow(Object[] row) {
    return new BookingsByActivityDateReportRow(
        asString(row[0]),
        asLong(row[1]),
        asString(row[2]),
        asLong(row[3]),
        asLocalDate(row[4]),
        asString(row[5]),
        asBigDecimal(row[6]),
        asZonedDateTime(row[7]),
        asBigDecimal(row[8]),
        asBigDecimal(row[9]),
        asBigDecimal(row[10]),
        asLong(row[11]),
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
        asString(row[22]),
        asString(row[23]),
        asString(row[24]));
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
