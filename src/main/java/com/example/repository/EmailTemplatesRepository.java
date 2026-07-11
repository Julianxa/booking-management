package com.example.repository;

import com.example.model.entity.EmailTemplates;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTemplatesRepository extends JpaRepository<EmailTemplates, Long> {

    @Query("SELECT et FROM EmailTemplates et WHERE et.templateHtmlFileName = 'booking-confirmation-email-template' AND et.isPerm = true")
    EmailTemplates findBookingConfirmationEmailTemplate();

    @Query("SELECT et FROM EmailTemplates et WHERE et.templateHtmlFileName = 'booking-order-summary-email-template' AND et.isPerm = true")
    EmailTemplates findBookingOrderSummaryEmailTemplate();

    @Query("SELECT et FROM EmailTemplates et WHERE et.templateHtmlFileName = 'booking-cancellation-email-template' AND et.isPerm = true")
    EmailTemplates findBookingCancellationEmailTemplate();

    @Query("SELECT et FROM EmailTemplates et WHERE et.templateHtmlFileName = 'booking-reminder-email-template' AND et.isPerm = true")
    EmailTemplates findBookingReminderEmailTemplate();

    Optional<EmailTemplates> findByRefNo(String emailTemplateRefNo);

    @Query("""
            SELECT new com.example.model.entity.EmailTemplates(
                et.id,
                et.refNo,
                et.templateHtmlFileName,
                et.title,
                et.titleZhCn,
                et.titleZhHk,
                et.subject,
                et.subjectZhCn,
                et.subjectZhHk,
                SUBSTRING(et.mainBody, 1, 200),
                SUBSTRING(et.mainBodyZhCn, 1, 200),
                SUBSTRING(et.mainBodyZhHk, 1, 200),
                SUBSTRING(et.importantInfoIntro, 1, 200),
                SUBSTRING(et.importantInfoIntroZhCn, 1, 200),
                SUBSTRING(et.importantInfoIntroZhHk, 1, 200),
                SUBSTRING(et.importantInfoBody, 1, 200),
                SUBSTRING(et.importantInfoBodyZhCn, 1, 200),
                SUBSTRING(et.importantInfoBodyZhHk, 1, 200),
                SUBSTRING(et.contactBody, 1, 200),
                SUBSTRING(et.contactBodyZhCn, 1, 200),
                SUBSTRING(et.contactBodyZhHk, 1, 200),
                et.reminderDayInterval,
                et.isPerm,
                et.createdAt, et.updatedAt
            )
            FROM EmailTemplates et
            """)
    Page<EmailTemplates> findAllActive(Pageable pageable);

    boolean existsByRefNo(String refNo);

    @Query("""
            SELECT et.reminderDayInterval
            FROM EmailTemplates et
            WHERE et.templateHtmlFileName = 'booking-reminder-email-template'
            """)
    Optional<Integer> findReminderDayInterval();
}
