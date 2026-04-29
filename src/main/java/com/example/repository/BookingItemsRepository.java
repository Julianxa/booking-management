package com.example.repository;

import com.example.model.entity.BookingItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BookingItemsRepository extends JpaRepository<BookingItems, Long> {
    List<BookingItems> findByBookingEventId(Long bookingEventId);
}
