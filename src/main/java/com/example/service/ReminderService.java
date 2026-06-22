package com.example.service;

import com.example.exception.email.MissingIntervalException;
import com.example.repository.BookingEventsRepository;
import com.example.repository.EmailTemplatesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {
    /**
     * Sentinel stored in {@code reminder_sent_at} while a worker sends emails.
     * Prevents duplicate dispatch across instances; released on failure for retry.
     */
    public static final ZonedDateTime REMINDER_CLAIMED_AT =
            ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

    private static final ZoneId HONG_KONG = ZoneId.of("Asia/Hong_Kong");

    private final EmailTemplatesRepository templatesRepository;
    private final BookingEventsRepository bookingEventsRepository;

    public LocalDate resolveReminderTargetDate() {
        Integer reminderInterval = templatesRepository
                .findReminderDayInterval()
                .orElseThrow(() -> new MissingIntervalException("Missing reminder_day_interval in template"));
        return LocalDate.now(HONG_KONG).plusDays(reminderInterval);
    }

    @Transactional
    public List<Long> claimNextReminderBatch(LocalDate targetDate, int batchSize) {
        List<Long> ids = bookingEventsRepository.findReminderCandidateIdsForUpdateSkipLocked(targetDate, batchSize);
        if (ids.isEmpty()) {
            return ids;
        }
        bookingEventsRepository.markReminderClaimed(ids, REMINDER_CLAIMED_AT);
        return ids;
    }

    @Transactional
    public void markReminderSent(Long bookingEventId) {
        ZonedDateTime sentAt = ZonedDateTime.now();
        int updated = bookingEventsRepository.markReminderSentFromClaim(
                bookingEventId, REMINDER_CLAIMED_AT, sentAt);
        if (updated == 0) {
            bookingEventsRepository.findById(bookingEventId)
                    .filter(be -> be.getReminderSentAt() == null)
                    .ifPresent(be -> {
                        be.setReminderSentAt(sentAt);
                        bookingEventsRepository.save(be);
                    });
        }
    }

    @Transactional
    public void releaseReminderClaim(Long bookingEventId) {
        bookingEventsRepository.releaseReminderClaim(bookingEventId, REMINDER_CLAIMED_AT);
    }
}
