package com.example.repository;

import com.example.model.entity.BookingItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BookingItemsRepository extends JpaRepository<BookingItems, Long> {
    List<BookingItems> findByBookingEventId(Long bookingEventId);

    @Query("SELECT COALESCE(SUM(bi.quantity), 0) FROM BookingItems bi WHERE bi.bookingEventId = :bookingEventId")
    Integer sumQuantityByBookingEventId(@Param("bookingEventId") Long bookingEventId);
}
