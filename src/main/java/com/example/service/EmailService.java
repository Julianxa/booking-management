package com.example.service;

import com.example.constant.Enums;
import com.example.exception.email.EmailTemplateNotFoundException;
import com.example.exception.email.OfficialTemplateDeletionException;
import com.example.exception.ticket.TicketTypeNotFoundException;
import com.example.mapper.EmailTemplateMapper;
import com.example.model.dto.*;
import com.example.model.entity.*;
import com.example.repository.EmailLogsRepository;
import com.example.repository.EmailTemplatesRepository;
import com.example.repository.TicketTypesRepository;
import com.example.utils.QRCodeGenerator;
import com.example.utils.ReferenceNoGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.example.constant.Enums.UserRole.AGENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final TemplateEngine templateEngine;
    private final EmailTemplatesRepository emailTemplatesRepository;
    private final TicketTypesRepository ticketTypesRepository;
    private final QRCodeGenerator qrCodeGenerator;
    private final JavaMailSender javaMailSender;
    private final EmailTemplateMapper emailTemplateMapper;
    private final AuditService auditService;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final EmailLogsRepository emailLogsRepository;
    @Value("${app.mail.from}")
    String senderEmail;

    public record BookingEmailPayload(
            CreateBookingRequestDTO.AttendeeDTO attendee,
            BookingEvents bookingEvent,
            List<CreateBookingRequestDTO.TicketTypeDTO> tickets,
            List<CreateBookingRequestDTO.AttendeeDTO> allAttendees
    ) {
    }

    public record BookingCreatedEvent(
            Users loggedInUser,
            Bookings booking,
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents,
            String promoCode,
            List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets,
            List<BookingEmailPayload> emailPayloads
    ) {
    }

    public record BookingRestoreEvent(
            Users loggedInUser,
            Bookings booking,
            List<BookingEmailPayload> emailPayloads
    ) {
    }

    public GetEmailTemplateResponseDTO getEmailTemplate(String emailTemplateRefNo) {
        EmailTemplates emailTemplate = emailTemplatesRepository.findByRefNo(emailTemplateRefNo)
                .orElseThrow(() -> new EmailTemplateNotFoundException(String.format("Email template not found with code %s", emailTemplateRefNo)));

        GetEmailTemplateResponseDTO getEmailTemplateResponseDTO = emailTemplateMapper.toResponseDTO(emailTemplate);

        getEmailTemplateResponseDTO.setMessage("Retrieve Email Templates successfully.");
        getEmailTemplateResponseDTO.setTimestamp(ZonedDateTime.now());
        return getEmailTemplateResponseDTO;
    }

    public GetListEmailTemplatesResponseDTO getAllEmailTemplates(Pageable pageable) {
        Page<EmailTemplates> emailTemplatesPage = emailTemplatesRepository.findAllActive(pageable);

        List<GetEmailTemplateResponseDTO> content = emailTemplatesPage.getContent().stream()
                .map(emailTemplateMapper::toResponseDTO)
                .toList();

        GetListEmailTemplatesResponseDTO getListEmailTemplatesResponseDTO = emailTemplateMapper.toGetListResponse(emailTemplatesPage, content);
        getListEmailTemplatesResponseDTO.setMessage("Retrieve list of Email Templates successfully.");
        getListEmailTemplatesResponseDTO.setTimestamp(ZonedDateTime.now());
        return getListEmailTemplatesResponseDTO;
    }

    @Transactional
    public CreateEmailTemplatesResponseDTO createEmailTemplate(CreateEmailTemplatesRequestDTO createEmailTemplatesRequestDTO) {
        EmailTemplates template = emailTemplateMapper.toEntity(createEmailTemplatesRequestDTO);
        template.setRefNo(referenceNoGenerator.generateEmailTemplateReference());
        emailTemplatesRepository.save(template);

        auditService.record("CREATE_EMAIL_TEMPLATE",
                EmailTemplates.class.getName(),
                template.getId(),
                null,
                template.getRefNo()
        );

        return emailTemplateMapper.toCreateResponseDTO(template);
    }

    @Transactional
    public CreateEmailTemplatesResponseDTO updateEmailTemplate(String templateRefNo, CreateEmailTemplatesRequestDTO updateEmailTemplatesRequestDTO) {

        EmailTemplates template = emailTemplatesRepository.findByRefNo(templateRefNo)
                .orElseThrow(() -> new EmailTemplateNotFoundException(String.format("Email template not found with code %s", templateRefNo)));

        if (updateEmailTemplatesRequestDTO.getTemplateName() != null && !template.getIsPerm())
            template.setTemplateName(updateEmailTemplatesRequestDTO.getTemplateName());
        if (updateEmailTemplatesRequestDTO.getTitle() != null)
            template.setTitle(updateEmailTemplatesRequestDTO.getTitle());
        if (updateEmailTemplatesRequestDTO.getTitleZhCn() != null)
            template.setTitleZhCn(updateEmailTemplatesRequestDTO.getTitleZhCn());
        if (updateEmailTemplatesRequestDTO.getTitleZhHk() != null)
            template.setTitleZhHk(updateEmailTemplatesRequestDTO.getTitleZhHk());
        if (updateEmailTemplatesRequestDTO.getSubject() != null)
            template.setSubject(updateEmailTemplatesRequestDTO.getSubject());
        if (updateEmailTemplatesRequestDTO.getSubjectZhCn() != null)
            template.setSubjectZhCn(updateEmailTemplatesRequestDTO.getSubjectZhCn());
        if (updateEmailTemplatesRequestDTO.getSubjectZhHk() != null)
            template.setSubjectZhHk(updateEmailTemplatesRequestDTO.getSubjectZhHk());
        if (updateEmailTemplatesRequestDTO.getMainBody() != null)
            template.setMainBody(updateEmailTemplatesRequestDTO.getMainBody());
        if (updateEmailTemplatesRequestDTO.getMainBodyZhCn() != null)
            template.setMainBodyZhCn(updateEmailTemplatesRequestDTO.getMainBodyZhCn());
        if (updateEmailTemplatesRequestDTO.getMainBodyZhHk() != null)
            template.setMainBodyZhHk(updateEmailTemplatesRequestDTO.getMainBodyZhHk());
        if (updateEmailTemplatesRequestDTO.getImportantInfoIntro() != null)
            template.setImportantInfoIntro(updateEmailTemplatesRequestDTO.getImportantInfoIntro());
        if (updateEmailTemplatesRequestDTO.getImportantInfoIntroZhCn() != null)
            template.setImportantInfoIntroZhCn(updateEmailTemplatesRequestDTO.getImportantInfoIntroZhCn());
        if (updateEmailTemplatesRequestDTO.getImportantInfoIntroZhHk() != null)
            template.setImportantInfoIntroZhHk(updateEmailTemplatesRequestDTO.getImportantInfoIntroZhHk());
        if (updateEmailTemplatesRequestDTO.getImportantInfoBody() != null)
            template.setImportantInfoBody(updateEmailTemplatesRequestDTO.getImportantInfoBody());
        if (updateEmailTemplatesRequestDTO.getImportantInfoBodyZhCn() != null)
            template.setImportantInfoBodyZhCn(updateEmailTemplatesRequestDTO.getImportantInfoBodyZhCn());
        if (updateEmailTemplatesRequestDTO.getImportantInfoBodyZhHk() != null)
            template.setImportantInfoBodyZhHk(updateEmailTemplatesRequestDTO.getImportantInfoBodyZhHk());
        if (updateEmailTemplatesRequestDTO.getContactBody() != null)
            template.setContactBody(updateEmailTemplatesRequestDTO.getContactBody());
        if (updateEmailTemplatesRequestDTO.getContactBodyZhCn() != null)
            template.setContactBodyZhCn(updateEmailTemplatesRequestDTO.getContactBodyZhCn());
        if (updateEmailTemplatesRequestDTO.getContactBodyZhHk() != null)
            template.setContactBodyZhHk(updateEmailTemplatesRequestDTO.getContactBodyZhHk());
        if (updateEmailTemplatesRequestDTO.getReminderDayInterval() != null)
            template.setReminderDayInterval(updateEmailTemplatesRequestDTO.getReminderDayInterval());
        template = emailTemplatesRepository.save(template);

        auditService.record("UPDATE_EMAIL_TEMPLATE",
                EmailTemplates.class.getName(),
                template.getId(),
                null,
                template.getRefNo()
        );

        return emailTemplateMapper.toCreateResponseDTO(template);
    }


    @Transactional
    public DeleteEmailTemplateResponseDTO deleteEmailTemplate(String emailTemplateRefNo) {
        EmailTemplates template = emailTemplatesRepository.findByRefNo(emailTemplateRefNo)
                .orElseThrow(() -> new EmailTemplateNotFoundException(String.format("Email template %s not found", emailTemplateRefNo)));

        if(template.getIsPerm()) {
            throw new OfficialTemplateDeletionException("Official template cannot be deleted");
        }

        emailTemplatesRepository.delete(template);

        ZonedDateTime deletedAt = ZonedDateTime.now();
        DeleteEmailTemplateResponseDTO deleteEmailTemplateResponseDTO = new DeleteEmailTemplateResponseDTO();
        deleteEmailTemplateResponseDTO.setMessage("Email template deleted successfully");
        deleteEmailTemplateResponseDTO.setTimestamp(deletedAt);

        auditService.record("DELETE_EMAIL_TEMPLATE",
                EmailTemplates.class.getName(),
                null,
                null,
                "Delete email template successfully"
        );
        return deleteEmailTemplateResponseDTO;
    }

    @Async
    public void sendBookingOrderSummaryEmail(Users user, Bookings booking, List<CreateBookingRequestDTO.BookingEventDTO> eventList, String giftCertificatePromoCode, List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTicketList) {
        Context context = new Context();

        Map<String, String> inlineImages = embedInlineImages();

        EmailTemplates template = emailTemplatesRepository.findBookingOrderSummaryEmailTemplate();

        context.setVariable("bookingId", booking.getRefNo());
        context.setVariable("grandTotal", booking.getTotalPaidPrice());
        context.setVariable("finalAmount", booking.getFinalPaidAmount());
        context.setVariable("currency", booking.getCurrency());

        context.setVariable("firstName", user.getFirstName());

        context.setVariable("bookingEvents", eventList);
        context.setVariable("redeemedTickets", redeemedTicketList);

        context.setVariable("giftCertificatePromoCode", giftCertificatePromoCode);
        context.setVariable("giftCertificateDiscount", booking.getDiscount());

        if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.CN) {
            context.setVariable("titleZhCn", template.getTitleZhCn());
            context.setVariable("subjectZhCn", template.getSubjectZhCn());
            context.setVariable("mainBodyZhCn", template.getMainBodyZhCn());
            context.setVariable("importantInfoIntroZhCn", template.getImportantInfoIntroZhCn());
            context.setVariable("importantInfoBodyZhCn", template.getImportantInfoBodyZhCn());
            context.setVariable("contactBodyZhCn", template.getContactBodyZhCn());
        } else if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.HK) {
            context.setVariable("titleZhHk", template.getTitleZhHk());
            context.setVariable("subjectZhHk", template.getSubjectZhHk());
            context.setVariable("mainBodyZhHk", template.getMainBodyZhHk());
            context.setVariable("importantInfoIntroZhHk", template.getImportantInfoIntroZhHk());
            context.setVariable("importantInfoBodyZhHk", template.getImportantInfoBodyZhHk());
            context.setVariable("contactBodyZhHk", template.getContactBodyZhHk());
        } else {
            context.setVariable("title", template.getTitle());
            context.setVariable("subject", template.getSubject());
            context.setVariable("mainBody", template.getMainBody());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntro());
            context.setVariable("importantInfoBody", template.getImportantInfoBody());
            context.setVariable("contactBody", template.getContactBody());
        }
        String htmlContent = templateEngine.process("booking-order-summary-email-template", context);

        String emailParametersJson = convertContextToJson(context);

        String subject = getEmailSubject(template, booking.getLanguage());

        sendEmail(user.getId(), template.getId(), emailParametersJson, user.getEmail(), subject, htmlContent, inlineImages);
    }

    @Async
    public void sendBookingConfirmationEmail(CreateBookingRequestDTO.AttendeeDTO attendeeDTO,
                                             Bookings booking,
                                             BookingEvents bookingEvent,
                                             List<CreateBookingRequestDTO.TicketTypeDTO> ticketsDTOs,
                                             List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs) {
        Context context = new Context();
        String checkInToken = bookingEvent.getVerificationToken();

        Map<String, String> inlineImages = embedInlineImages();
        inlineImages.put("qr", checkInToken);

        EmailTemplates template = emailTemplatesRepository.findBookingConfirmationEmailTemplate();

        String ticketSummary = buildTicketSummary(ticketsDTOs);

        context.setVariable("attendees", attendeeDTOs);

        context.setVariable("ticketSummary", ticketSummary);

        context.setVariable("bookingId", booking.getRefNo());

        context.setVariable("firstName", attendeeDTO.getFirstName());

        context.setVariable("eventName", bookingEvent.getEvent().getName());
        context.setVariable("eventDate", bookingEvent.getEventDate());
        context.setVariable("eventTime", bookingEvent.getEventTime());
        context.setVariable("bookingEventTotal", bookingEvent.getTotal());

        if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.CN) {
            context.setVariable("titleZhCn", template.getTitleZhCn());
            context.setVariable("subjectZhCn", template.getSubjectZhCn());
            context.setVariable("mainBodyZhCn", template.getMainBodyZhCn());
            context.setVariable("importantInfoIntroZhCn", template.getImportantInfoIntroZhCn());
            context.setVariable("importantInfoBodyZhCn", template.getImportantInfoBodyZhCn());
            context.setVariable("contactBodyZhCn", template.getContactBodyZhCn());

        } else if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.HK) {
            context.setVariable("titleZhHk", template.getTitleZhHk());
            context.setVariable("subjectZhHk", template.getSubjectZhHk());
            context.setVariable("mainBodyZhHk", template.getMainBodyZhHk());
            context.setVariable("importantInfoIntroZhHk", template.getImportantInfoIntroZhHk());
            context.setVariable("importantInfoBodyZhHk", template.getImportantInfoBodyZhHk());
            context.setVariable("contactBodyZhHk", template.getContactBodyZhHk());

        } else {
            context.setVariable("title", template.getTitle());
            context.setVariable("subject", template.getSubject());
            context.setVariable("mainBody", template.getMainBody());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntro());
            context.setVariable("importantInfoBody", template.getImportantInfoBody());
            context.setVariable("contactBody", template.getContactBody());
        }
        String htmlContent = templateEngine.process("booking-confirmation-email-template", context);

        String emailParametersJson = convertContextToJson(context);

        String subject = getEmailSubject(template, booking.getLanguage());

        sendEmail(null, template.getId(), emailParametersJson, attendeeDTO.getEmail(), subject, htmlContent, inlineImages);
    }

    @Async
    public void sendBookingCancellationEmail(CreateBookingRequestDTO.AttendeeDTO attendeeDTO,
                                             Bookings booking,
                                             BookingEvents bookingEvent,
                                             List<CreateBookingRequestDTO.TicketTypeDTO> ticketsDTOs,
                                             List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs) {
        Context context = new Context();

        Map<String, String> inlineImages = embedInlineImages();

        EmailTemplates template = emailTemplatesRepository.findBookingCancellationEmailTemplate();

        String ticketSummary = buildTicketSummary(ticketsDTOs);

        context.setVariable("attendees", attendeeDTOs);

        context.setVariable("ticketSummary", ticketSummary);

        context.setVariable("bookingId", booking.getRefNo());

        context.setVariable("firstName", attendeeDTO.getFirstName());

        context.setVariable("eventName", bookingEvent.getEvent().getName());
        context.setVariable("eventDate", bookingEvent.getEventDate());
        context.setVariable("eventTime", bookingEvent.getEventTime());
        context.setVariable("bookingEventTotal", bookingEvent.getTotal());

        if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.CN) {
            context.setVariable("titleZhCn", template.getTitleZhCn());
            context.setVariable("subjectZhCn", template.getSubjectZhCn());
            context.setVariable("mainBodyZhCn", template.getMainBodyZhCn());
            context.setVariable("importantInfoIntroZhCn", template.getImportantInfoIntroZhCn());
            context.setVariable("importantInfoBodyZhCn", template.getImportantInfoBodyZhCn());
            context.setVariable("contactBodyZhCn", template.getContactBodyZhCn());

        } else if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.HK) {
            context.setVariable("titleZhHk", template.getTitleZhHk());
            context.setVariable("subjectZhHk", template.getSubjectZhHk());
            context.setVariable("mainBodyZhHk", template.getMainBodyZhHk());
            context.setVariable("importantInfoIntroZhHk", template.getImportantInfoIntroZhHk());
            context.setVariable("importantInfoBodyZhHk", template.getImportantInfoBodyZhHk());
            context.setVariable("contactBodyZhHk", template.getContactBodyZhHk());

        } else {
            context.setVariable("title", template.getTitle());
            context.setVariable("subject", template.getSubject());
            context.setVariable("mainBody", template.getMainBody());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntro());
            context.setVariable("importantInfoBody", template.getImportantInfoBody());
            context.setVariable("contactBody", template.getContactBody());
        }
        String htmlContent = templateEngine.process("booking-cancellation-email-template", context);

        String emailParametersJson = convertContextToJson(context);

        String subject = getEmailSubject(template, booking.getLanguage());

        sendEmail(null, template.getId(), emailParametersJson, attendeeDTO.getEmail(), subject, htmlContent, inlineImages);
    }

    @Async
    public void sendBookingReminderEmail(CreateBookingRequestDTO.AttendeeDTO attendeeDTO,
                                             Bookings booking,
                                             BookingEvents bookingEvent,
                                             List<CreateBookingRequestDTO.TicketTypeDTO> ticketsDTOs,
                                             List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs) {
        Context context = new Context();

        Map<String, String> inlineImages = embedInlineImages();

        EmailTemplates template = emailTemplatesRepository.findBookingReminderEmailTemplate();

        String ticketSummary = buildTicketSummary(ticketsDTOs);

        context.setVariable("attendees", attendeeDTOs);

        context.setVariable("ticketSummary", ticketSummary);

        context.setVariable("bookingId", booking.getRefNo());

        context.setVariable("firstName", attendeeDTO.getFirstName());

        context.setVariable("eventName", bookingEvent.getEvent().getName());
        context.setVariable("eventDate", bookingEvent.getEventDate());
        context.setVariable("eventTime", bookingEvent.getEventTime());
        context.setVariable("bookingEventTotal", bookingEvent.getTotal());

        if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.CN) {
            context.setVariable("titleZhCn", template.getTitleZhCn());
            context.setVariable("subjectZhCn", template.getSubjectZhCn());
            context.setVariable("mainBodyZhCn", template.getMainBodyZhCn());
            context.setVariable("importantInfoIntroZhCn", template.getImportantInfoIntroZhCn());
            context.setVariable("importantInfoBodyZhCn", template.getImportantInfoBodyZhCn());
            context.setVariable("contactBodyZhCn", template.getContactBodyZhCn());

        } else if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.HK) {
            context.setVariable("titleZhHk", template.getTitleZhHk());
            context.setVariable("subjectZhHk", template.getSubjectZhHk());
            context.setVariable("mainBodyZhHk", template.getMainBodyZhHk());
            context.setVariable("importantInfoIntroZhHk", template.getImportantInfoIntroZhHk());
            context.setVariable("importantInfoBodyZhHk", template.getImportantInfoBodyZhHk());
            context.setVariable("contactBodyZhHk", template.getContactBodyZhHk());

        } else {
            context.setVariable("title", template.getTitle());
            context.setVariable("subject", template.getSubject());
            context.setVariable("mainBody", template.getMainBody());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntro());
            context.setVariable("importantInfoBody", template.getImportantInfoBody());
            context.setVariable("contactBody", template.getContactBody());
        }
        String htmlContent = templateEngine.process("booking-reminder-email-template", context);

        String emailParametersJson = convertContextToJson(context);

        String subject = getEmailSubject(template, booking.getLanguage());

        sendEmail(null, template.getId(), emailParametersJson, attendeeDTO.getEmail(), subject, htmlContent, inlineImages);
    }


    public String buildTicketSummary(List<CreateBookingRequestDTO.TicketTypeDTO> ticketTypesDTOs) {
        if (ticketTypesDTOs == null || ticketTypesDTOs.isEmpty()) {
            return "No tickets selected";
        }

        return ticketTypesDTOs.stream()
                .filter(dto -> dto.getQuantity() > 0)
                .map(dto -> {

                    TicketTypes ticketType = ticketTypesRepository.findByRefNo(dto.getId())
                            .orElseThrow(() -> new TicketTypeNotFoundException(String.format("Ticket Type %s not found", dto.getId())));
                    String name = ticketType.getName();
                    return name + " x " + dto.getQuantity();
                })
                .collect(Collectors.joining(", "));
    }

    private void sendEmail(Long userId, Long templateId, String emailParametersJson, String to, String subject, String htmlContent, Map<String, String> inlineImages) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setText(htmlContent, true);
            helper.setTo(to);
            helper.setFrom(senderEmail);
            helper.setSubject(subject);
            for (Map.Entry<String, String> entry : inlineImages.entrySet()) {
                if (entry.getKey().equals("qr")) {
                    byte[] qrBytes = qrCodeGenerator.generateQrCode(entry.getValue());
                    ByteArrayResource qrResource = new ByteArrayResource(qrBytes);

                    helper.addInline(entry.getKey(), qrResource, "image/png");
                } else
                    helper.addInline(entry.getKey(), new ClassPathResource(entry.getValue()));
            }
            javaMailSender.send(message);

            recordEmailResult(userId, templateId, emailParametersJson, Enums.EmailStatus.SUCCESS, "Email sent successfully");
        } catch (MessagingException | MailException e) {
            recordEmailResult(userId, templateId, emailParametersJson, Enums.EmailStatus.FAILED, "Failed to send email");
            log.error("Failed to send email to {} with subject '{}'", to, subject, e);
        }
    }

    private void recordEmailResult(Long userId,
                                   Long templateId,
                                   String emailParametersJson,
                                   Enums.EmailStatus status,
                                   String auditMessage) {
        EmailLogs logs = EmailLogs.builder()
                .userId(userId)
                .emailParameters(emailParametersJson)
                .templateId(templateId)
                .status(status)
                .build();

        emailLogsRepository.save(logs);

        auditService.record("SEND_EMAIL",
                Bookings.class.getName(),
                null,
                null,
                auditMessage
        );
    }

    private Map<String, String> embedInlineImages() {
        Map<String, String> inlineImages = new HashMap<>();
        inlineImages.put("logo", "images/email/logo.png");
        inlineImages.put("google", "images/email/google.png");
        inlineImages.put("apple", "images/email/apple.png");
        inlineImages.put("cat", "images/email/cat.png");
        inlineImages.put("fb", "images/email/fb.png");
        inlineImages.put("wb", "images/email/wb.png");
        inlineImages.put("ta", "images/email/ta.png");
        inlineImages.put("ig", "images/email/ig.png");
        inlineImages.put("yt", "images/email/yt.png");
        return inlineImages;
    }

    public void sendBookingConfirmationEmailsAsync(Bookings booking, List<BookingEmailPayload> payloads) {
        for (BookingEmailPayload payload : payloads) {
            sendBookingConfirmationEmail(
                    payload.attendee(),
                    booking,
                    payload.bookingEvent(),
                    payload.tickets(),
                    payload.allAttendees()
            );
        }
    }

    void sendBookingOrderSummaryEmailsAsync(Users loggedInUser,
                                            Bookings booking,
                                            List<CreateBookingRequestDTO.BookingEventDTO> eventList,
                                            String promoCode,
                                            List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets,
                                            List<BookingEmailPayload> payloads) {
        if (loggedInUser != null && loggedInUser.getRole() == AGENT) {
            sendBookingOrderSummaryEmail(loggedInUser, booking, eventList, promoCode, redeemedTickets);
        } else {
            for (BookingEmailPayload payload : payloads) {
                if (payload.attendee().getSequence() == 1) {
                    Users guestAttendee = new Users();
                    guestAttendee.setEmail(payload.attendee().getEmail());
                    guestAttendee.setFirstName(payload.attendee.getFirstName());
                    sendBookingOrderSummaryEmail(guestAttendee, booking, eventList, promoCode, redeemedTickets);
                }
            }
        }
    }

    void sendBookingCancellationEmailsAsync(Bookings booking, List<EmailService.BookingEmailPayload> payloads) {
        for (EmailService.BookingEmailPayload payload : payloads) {
            sendBookingCancellationEmail(
                    payload.attendee(),
                    booking,
                    payload.bookingEvent(),
                    payload.tickets(),
                    payload.allAttendees()
            );
        }
    }

    private String convertContextToJson(Context context) {
        try {
            Map<String, Object> variables = new HashMap<>();

            for (String key : context.getVariableNames()) {
                variables.put(key, context.getVariable(key));
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());           // 支援 LocalDateTime 等
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

            return objectMapper.writeValueAsString(variables);

        } catch (Exception e) {
            log.warn("Failed to convert Thymeleaf Context to JSON", e);
            return "{}";
        }
    }

    private String getEmailSubject(EmailTemplates template, Enums.Language language) {
        if (language == Enums.Language.CN && template.getSubjectZhCn() != null) {
            return template.getSubjectZhCn();
        } else if (language == Enums.Language.HK && template.getSubjectZhHk() != null) {
            return template.getSubjectZhHk();
        }
        return template.getSubject();
    }
}