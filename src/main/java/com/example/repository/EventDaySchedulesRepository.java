package com.example.repository;

import com.example.model.entity.EventDaySchedules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventDaySchedulesRepository extends JpaRepository<EventDaySchedules, Long> {
    @Query("""
        SELECT s FROM EventDaySchedules s 
        WHERE s.id.eventId = :eventId 
          AND s.id.day = :day 
          AND s.id.startTime = :startTime
        """)
    Optional<EventDaySchedules> findByEventIdAndDayAndStartTime(
            @Param("eventId") Long eventId,
            @Param("day") String day,
            @Param("startTime") String startTime
    );

    List<EventDaySchedules> findByIdEventIdAndIdDay(Long eventId, String day);

    @Query("""
        SELECT s FROM EventDaySchedules s 
        WHERE s.id.eventId = :eventId 
        ORDER BY s.id.day, s.id.startTime
        """)
    List<EventDaySchedules> findByEventIdOrderByDayAndTime(@Param("eventId") Long eventId);
}
