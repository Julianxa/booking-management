package com.example.scheduler;

import com.example.service.ReminderDispatchService;
import com.example.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ReminderScheduler {
    private final ReminderService reminderService;
    private final ReminderDispatchService reminderDispatchService;

    @Value("${app.reminder.batch-size:100}")
    private int batchSize;

    @Value("${app.reminder.run-on-startup:false}")
    private boolean runOnStartup;

    @Scheduled(cron = "${app.reminder.cron:0 0 9 * * *}", zone = "${app.reminder.zone:Asia/Hong_Kong}")
    public void sendOneDayBeforeReminders() {
        LocalDate targetDate = reminderService.resolveReminderTargetDate();
        log.info("Starting scheduled reminder dispatch for target date {}", targetDate);
        dispatchRemindersForTargetDate(targetDate);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sendRemindersOnStartup() {
        if (!runOnStartup) {
            return;
        }
        LocalDate targetDate = reminderService.resolveReminderTargetDate();
        log.info("Application started — dispatching pending reminders for target date {}", targetDate);
        dispatchRemindersForTargetDate(targetDate);
    }

    private void dispatchRemindersForTargetDate(LocalDate targetDate) {
        int totalDispatched = 0;

        while (true) {
            List<Long> claimedIds = reminderService.claimNextReminderBatch(targetDate, batchSize);
            if (claimedIds.isEmpty()) {
                break;
            }

            for (Long bookingEventId : claimedIds) {
                reminderDispatchService.dispatchReminderForEvent(bookingEventId);
            }

            totalDispatched += claimedIds.size();

            if (claimedIds.size() < batchSize) {
                break;
            }
        }

        log.info("Queued {} reminder job(s) for target date {} (async workers will send emails)",
                totalDispatched, targetDate);
    }
}
