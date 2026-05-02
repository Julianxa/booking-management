package com.example.repository;

import com.example.model.entity.Events;
import com.example.model.record.EventBookingSummary;
import com.example.model.record.EventDailySlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventsRepository extends JpaRepository<Events, Long> {
    @Query("SELECT e.id FROM Events e WHERE e.refNo = :refNo")
    Optional<Long> findIdByRefNo(String refNo);

    @Query("SELECT e FROM Events e WHERE e.refNo = :refNo")
    Optional<Events> findByRefNo(String refNo);

    @Query("SELECT e FROM Events e WHERE e.refNo = :refNo AND e.deletedAt IS NULL AND e.isPublish = true")
    Optional<Events> findByRefNoAndOpenStatusAndPublished(String refNo);

    @Query("SELECT e.refNo FROM Events e WHERE e.id = :id")
    Optional<String> findRefNoById(Long id);

    @Query("""
    SELECT e FROM Events e
    WHERE e.deletedAt IS NULL
    AND (:isPublishedOnly = false OR e.isPublish = true)
    AND (
    LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
       OR LOWER(e.type) LIKE LOWER(CONCAT('%', :search, '%'))
       OR LOWER(e.category) LIKE LOWER(CONCAT('%', :search, '%'))
       OR LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Events> findBySearchTermWithPublishFilter(@Param("publishedOnly") Boolean isPublishedOnly, @Param("search") String search, Pageable pageable);

    @Query("""
    SELECT e
    FROM Events e
    WHERE e.deletedAt IS NULL
    """)
    Page<Events> findAllActive(Pageable pageable);

    @Query("""
    SELECT e
    FROM Events e
    WHERE e.deletedAt IS NULL
    AND (:isPublishedOnly = false OR e.isPublish = true)
    """)
    Page<Events> findAllPublished(@Param("isPublishedOnly") boolean isPublishedOnly, Pageable pageable);

    @Query("""
    SELECT e FROM Events e
    WHERE e.startDate <= :filterDate
      AND e.endDate >= :filterDate
    """)
    Page<Events> findByDate(@Param("filterDate") LocalDate filterDate, Pageable pageable);

    @Query("""
    SELECT e FROM Events e
    WHERE e.id = :id
      AND e.startDate <= :filterDate
      AND e.endDate >= :filterDate
    """)
    Events findByDateAndId(@Param("id") Long id, @Param("filterDate") LocalDate filterDate);

    @Query("""
    SELECT e FROM Events e
    WHERE (:filterDate IS NULL
           OR (e.startDate <= :filterDate AND e.endDate >= :filterDate))
      AND (
            LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(e.type) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(e.category) LIKE LOWER(CONCAT('%', :search, '%'))
         OR LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    ORDER BY e.startDate ASC
    """)
    Page<Events> findByDateAndSearch(
            @Param("filterDate") LocalDate filterDate,
            @Param("search") String search,
            Pageable pageable);

    @Query(value = """
    SELECT
        COALESCE(SUM(bi.quantity), 0) AS total_booked,
        COALESCE(SUM(CASE WHEN be.status = 'CHECKED_IN' THEN bi.quantity ELSE 0 END), 0) AS total_used
    FROM booking_events be
    LEFT JOIN booking_items bi ON be.id = bi.booking_event_id
    WHERE be.event_id = :eventId
      AND be.event_date = :filterDate
      AND be.event_time = :eventTime
    """, nativeQuery = true)
    EventBookingSummary getBookingSummary(
            @Param("eventId") Long eventId,
            @Param("filterDate") LocalDate filterDate,
            @Param("eventTime") String eventTime);

    @Query(value = """
    SELECT
        e.id AS event_id,
        e.ref_no AS event_ref,
        e.name AS event_name,
        eds.day AS schedule_day,
        :filterDate AS event_date,
        eds.start_time AS event_time,
        COALESCE(e.max_capacity, 0) AS max_capacity
    FROM events e
    INNER JOIN event_day_schedules eds ON e.id = eds.event_id
    WHERE e.deleted_at IS NULL
      AND (
          :isPublishedOnly = false
          OR e.is_publish = true
      )
      AND eds.day = :dayValue
    ORDER BY e.id, eds.start_time
    """, nativeQuery = true)
    List<EventDailySlot> getAllEventsScheduleSlots(
            @Param("isPublishedOnly") boolean isPublishedOnly,
            @Param("filterDate") LocalDate filterDate,
            @Param("dayValue") String dayValue
            );

    @Query(value = """
            SELECT
                e.id AS event_id,
                e.ref_no AS event_ref,
                e.name AS event_name,
                eds.day AS schedule_day,
                :filterDate AS event_date,
                eds.start_time AS event_time,
                COALESCE(e.max_capacity, 0) AS max_capacity
            FROM events e
            INNER JOIN event_day_schedules eds ON e.id = eds.event_id
            WHERE e.id = :id
              AND e.deleted_at IS NULL
              AND (
                  :isPublishedOnly = false
                  OR e.is_publish = true
              )
              AND eds.day = :dayValue
            ORDER BY eds.start_time
            """, nativeQuery = true)
    List<EventDailySlot> getEventScheduleSlots(
            @Param("isPublishedOnly") boolean isPublishedOnly,
            @Param("id") Long id,
            @Param("filterDate") LocalDate filterDate,
            @Param("dayValue") String dayValue);
}
