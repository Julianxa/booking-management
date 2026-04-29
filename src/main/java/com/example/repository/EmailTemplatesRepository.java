package com.example.repository;

import com.example.model.entity.EmailTemplates;
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

    Optional<EmailTemplates> findByRefNo(String emailTemplateRefNo);
}
