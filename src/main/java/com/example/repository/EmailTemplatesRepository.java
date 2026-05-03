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

    @Query("SELECT et FROM EmailTemplates et WHERE et.templateName = 'booking-confirmation-email-template'")
    EmailTemplates findBookingConfirmationEmailTemplate();

    @Query("SELECT et FROM EmailTemplates et WHERE et.templateName = 'booking-order-summary-email-template'")
    EmailTemplates findBookingOrderSummaryEmailTemplate();

    @Query("SELECT et FROM EmailTemplates et WHERE et.templateName = 'booking-cancellation-email-template'")
    EmailTemplates findBookingCancellationEmailTemplate();

    Optional<EmailTemplates> findByRefNo(String emailTemplateRefNo);

    @Query("""
    SELECT new com.example.model.entity.EmailTemplates(
        et.id,
        et.refNo,
        et.templateName,
        et.subject,
        SUBSTRING(et.mainBody, 1, 200),
        SUBSTRING(et.importantInfoIntro, 1, 200),
        SUBSTRING(et.importantInfoBody, 1, 200),
        SUBSTRING(et.contactBody, 1, 200),
        et.createdAt, et.updatedAt
    )
    FROM EmailTemplates et
    """)
    Page<EmailTemplates> findAllActive(Pageable pageable);

}
