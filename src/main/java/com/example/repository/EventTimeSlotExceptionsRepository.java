package com.example.repository;

import com.example.model.entity.EventTimeSlotExceptions;
import com.example.model.record.EventTimeSlotException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventTimeSlotExceptionsRepository extends JpaRepository<EventTimeSlotExceptions, Long> {
    @Query(value = """
            DELETE FROM event_time_slot_exceptions
            WHERE event_id = :eventId
              AND exception_date = :exceptionDate
              AND exception_time = :exceptionTime;
            """, nativeQuery = true)
    void deleteExceptionTimeByEventIdAndDateAndTime(Long eventId, LocalDate exceptionDate, String exceptionTime);

    @Query(value = """
    SELECT
        e.ref_no AS event_id,
        etse.exception_time AS event_time
    FROM events e
    INNER JOIN event_time_slot_exceptions etse ON e.id = etse.event_id
    WHERE e.deleted_at IS NULL
      AND e.id = :eventId
      AND etse.exception_date = :exceptionDate
    """, nativeQuery = true)
    List<EventTimeSlotException> findExceptionTimeByEventIdAndExceptionDate(Long eventId, LocalDate exceptionDate);

    @Query(value = """
    SELECT
        e.ref_no AS event_id,
        etse.exception_time AS event_time
    FROM events e
    INNER JOIN event_time_slot_exceptions etse ON e.id = etse.event_id
    WHERE e.deleted_at IS NULL
      AND etse.exception_date = :exceptionDate
    """, nativeQuery = true)
    List<EventTimeSlotException> findExceptionTimeByExceptionDate(LocalDate exceptionDate);
}
