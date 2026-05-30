package com.example.repository;

import com.example.model.entity.EventTimeSlotExceptionsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventTimeSlotExceptionsHistoryRepository extends JpaRepository<EventTimeSlotExceptionsHistory, Long> {
    Optional<List<EventTimeSlotExceptionsHistory>> findAllByEventIdAndExceptionDateAndExceptionTimeOrderByIdAsc(Long eventId, LocalDate eventDate, String eventTime);
}
