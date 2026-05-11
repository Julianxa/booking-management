package com.example.converter;

import com.example.model.entity.BookingEvents;
import com.example.model.entity.BookingItems;
import com.example.repository.BookingItemsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingEventsConverter {
    private final BookingItemsRepository bookingItemsRepository;

    public Map<Long, List<BookingItems>> toBookingItemsByEventMap(List<BookingEvents> bookingEvents) {
        Map<Long, List<BookingItems>> map = new HashMap<>();
        for (BookingEvents bookingEvent : bookingEvents) {
            map.put(bookingEvent.getId(), bookingItemsRepository.findByBookingEventId(bookingEvent.getId()));
        }
        return map;
    }
}
