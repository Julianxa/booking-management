package com.example.service;

import com.example.config.AppProperties;
import com.example.constant.Enums;
import com.example.exception.email.EmailProcessException;
import com.example.exception.email.EmailTemplateNotFoundException;
import com.example.exception.email.OfficialTemplateDeletionException;
import com.example.mapper.EmailTemplateMapper;
import com.example.mapper.TicketTypeMapper;
import com.example.model.dto.*;
import com.example.model.entity.*;
import com.example.repository.EmailLogsRepository;
import com.example.repository.EmailTemplatesRepository;
import com.example.utils.PartialUpdateUtil;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final TemplateEngine templateEngine;
    private final EmailTemplatesRepository emailTemplatesRepository;
    private final QRCodeGenerator qrCodeGenerator;
    private final JavaMailSender javaMailSender;
    private final EmailTemplateMapper emailTemplateMapper;
    private final AuditService auditService;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final EmailLogsRepository emailLogsRepository;
    private final AppProperties appProperties;

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

    public EmailTemplates resolveEmailTemplate(String emailTemplateRefNo) {
        if (emailTemplateRefNo == null || emailTemplateRefNo.isBlank()) {
            return null;
        }
        return emailTemplatesRepository.findByRefNo(emailTemplateRefNo)
                .orElseThrow(() -> new EmailTemplateNotFoundException(
                        String.format("Email template not found with code %s", emailTemplateRefNo)));
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
    public CreateEmailTemplatesResponseDTO updateEmailTemplate(String templateRefNo, UpdateEmailTemplatesRequestDTO dto) {

        EmailTemplates template = emailTemplatesRepository.findByRefNo(templateRefNo)
                .orElseThrow(() -> new EmailTemplateNotFoundException(String.format("Email template not found with code %s", templateRefNo)));

        PartialUpdateUtil.ifPresent(dto, "template_name", () -> {
            if (!template.getIsPerm()) {
                template.setTemplateName(dto.getTemplateName());
            }
        });
        PartialUpdateUtil.apply(dto, "title", dto::getTitle, template::setTitle);
        PartialUpdateUtil.apply(dto, "title_zh_cn", dto::getTitleZhCn, template::setTitleZhCn);
        PartialUpdateUtil.apply(dto, "title_zh_hk", dto::getTitleZhHk, template::setTitleZhHk);
        PartialUpdateUtil.apply(dto, "subject", dto::getSubject, template::setSubject);
        PartialUpdateUtil.apply(dto, "subject_zh_cn", dto::getSubjectZhCn, template::setSubjectZhCn);
        PartialUpdateUtil.apply(dto, "subject_zh_hk", dto::getSubjectZhHk, template::setSubjectZhHk);
        PartialUpdateUtil.apply(dto, "main_body", dto::getMainBody, template::setMainBody);
        PartialUpdateUtil.apply(dto, "main_body_zh_cn", dto::getMainBodyZhCn, template::setMainBodyZhCn);
        PartialUpdateUtil.apply(dto, "main_body_zh_hk", dto::getMainBodyZhHk, template::setMainBodyZhHk);
        PartialUpdateUtil.apply(dto, "important_info_intro", dto::getImportantInfoIntro, template::setImportantInfoIntro);
        PartialUpdateUtil.apply(dto, "important_info_intro_zh_cn", dto::getImportantInfoIntroZhCn, template::setImportantInfoIntroZhCn);
        PartialUpdateUtil.apply(dto, "important_info_intro_zh_hk", dto::getImportantInfoIntroZhHk, template::setImportantInfoIntroZhHk);
        PartialUpdateUtil.apply(dto, "important_info_body", dto::getImportantInfoBody, template::setImportantInfoBody);
        PartialUpdateUtil.apply(dto, "important_info_body_zh_cn", dto::getImportantInfoBodyZhCn, template::setImportantInfoBodyZhCn);
        PartialUpdateUtil.apply(dto, "important_info_body_zh_hk", dto::getImportantInfoBodyZhHk, template::setImportantInfoBodyZhHk);
        PartialUpdateUtil.apply(dto, "contact_body", dto::getContactBody, template::setContactBody);
        PartialUpdateUtil.apply(dto, "contact_body_zh_cn", dto::getContactBodyZhCn, template::setContactBodyZhCn);
        PartialUpdateUtil.apply(dto, "contact_body_zh_hk", dto::getContactBodyZhHk, template::setContactBodyZhHk);
        PartialUpdateUtil.apply(dto, "reminder_day_interval", dto::getReminderDayInterval, template::setReminderDayInterval);
        emailTemplatesRepository.save(template);

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

    public void sendPaymentConfirmationEmail(Users user, Bookings booking, List<CreateBookingRequestDTO.BookingEventDTO> eventList, String giftCertificatePromoCode, List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTicketList) {
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
        context.setVariable("activityDetailsUrlPrefix", buildActivityDetailsUrlPrefix(booking.getLanguage()));

        context.setVariable("giftCertificatePromoCode", giftCertificatePromoCode);
        context.setVariable("giftCertificateDiscount", booking.getDiscount());

        if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.CN) {
            context.setVariable("lang", Enums.Language.CN.name());
            context.setVariable("title", template.getTitleZhCn());
            context.setVariable("subject", template.getSubjectZhCn());
            context.setVariable("mainBody", template.getMainBodyZhCn());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntroZhCn());
            context.setVariable("importantInfoBody", template.getImportantInfoBodyZhCn());
            context.setVariable("contactBody", template.getContactBodyZhCn());
        } else if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.HK) {
            context.setVariable("lang", Enums.Language.HK.name());
            context.setVariable("title", template.getTitleZhHk());
            context.setVariable("subject", template.getSubjectZhHk());
            context.setVariable("mainBody", template.getMainBodyZhHk());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntroZhHk());
            context.setVariable("importantInfoBody", template.getImportantInfoBodyZhHk());
            context.setVariable("contactBody", template.getContactBodyZhHk());
        } else {
            context.setVariable("lang", Enums.Language.EN.name());
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

    public void sendCustomOrBookingConfirmationEmail(
            CreateBookingRequestDTO.AttendeeDTO attendeeDTO,
            Bookings booking,
            BookingEvents bookingEvent,
            List<CreateBookingRequestDTO.TicketTypeDTO> ticketsDTOs,
            List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs,
            String emailTemplateRefNo) {
        Context context = new Context();
        String checkInToken = bookingEvent.getVerificationToken();

        Map<String, String> inlineImages = embedInlineImages();
        inlineImages.put("qr", checkInToken);

        // static email template in default
        EmailTemplates template = emailTemplatesRepository.findBookingConfirmationEmailTemplate();
        if (emailTemplateRefNo != null && !emailTemplateRefNo.isBlank()) {
            template = resolveEmailTemplate(emailTemplateRefNo);
        } else if (bookingEvent.getEvent().getEmailTemplate() != null) {
            template = resolveEmailTemplate(bookingEvent.getEvent().getEmailTemplate().getRefNo());
        }

        String ticketSummary = buildTicketSummary(ticketsDTOs, booking.getLanguage());

        context.setVariable("attendees", attendeeDTOs);

        context.setVariable("ticketSummary", ticketSummary);

        context.setVariable("bookingId", booking.getRefNo());

        context.setVariable("firstName", attendeeDTO.getFirstName());

        context.setVariable("eventDate", bookingEvent.getEventDate());
        context.setVariable("eventTime", bookingEvent.getEventTime());
        context.setVariable("bookingEventTotal", bookingEvent.getTotal());
        context.setVariable(
            "activityDetailsUrl",
            buildActivityDetailsUrl(bookingEvent.getEvent(), booking.getLanguage()));

        if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.CN) {
            context.setVariable("lang", Enums.Language.CN.name());
            context.setVariable("eventName", bookingEvent.getEvent().getNameZhCn());
            context.setVariable("title", template.getTitleZhCn());
            context.setVariable("subject", template.getSubjectZhCn());
            context.setVariable("mainBody", template.getMainBodyZhCn());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntroZhCn());
            context.setVariable("importantInfoBody", template.getImportantInfoBodyZhCn());
            context.setVariable("contactBody", template.getContactBodyZhCn());

        } else if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.HK) {
            context.setVariable("lang", Enums.Language.HK.name());
            context.setVariable("eventName", bookingEvent.getEvent().getNameZhHk());
            context.setVariable("title", template.getTitleZhHk());
            context.setVariable("subject", template.getSubjectZhHk());
            context.setVariable("mainBody", template.getMainBodyZhHk());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntroZhHk());
            context.setVariable("importantInfoBody", template.getImportantInfoBodyZhHk());
            context.setVariable("contactBody", template.getContactBodyZhHk());

        } else {
            context.setVariable("lang", Enums.Language.EN.name());
            context.setVariable("eventName", bookingEvent.getEvent().getName());
            context.setVariable("title", template.getTitle());
            context.setVariable("subject", template.getSubject());
            context.setVariable("mainBody", template.getMainBody());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntro());
            context.setVariable("importantInfoBody", template.getImportantInfoBody());
            context.setVariable("contactBody", template.getContactBody());
        }
        String htmlContent = templateEngine.process(template.getTemplateName(), context);

        String emailParametersJson = convertContextToJson(context);

        String subject = getEmailSubject(template, booking.getLanguage());

        sendEmail(null, template.getId(), emailParametersJson, attendeeDTO.getEmail(), subject, htmlContent, inlineImages);
    }

    public void sendBookingCancellationEmail(CreateBookingRequestDTO.AttendeeDTO attendeeDTO,
                                             Bookings booking,
                                             BookingEvents bookingEvent,
                                             List<CreateBookingRequestDTO.TicketTypeDTO> ticketsDTOs,
                                             List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs) {
        Context context = new Context();

        Map<String, String> inlineImages = embedInlineImages();

        EmailTemplates template = emailTemplatesRepository.findBookingCancellationEmailTemplate();

        String ticketSummary = buildTicketSummary(ticketsDTOs, booking.getLanguage());

        context.setVariable("attendees", attendeeDTOs);

        context.setVariable("ticketSummary", ticketSummary);

        context.setVariable("bookingId", booking.getRefNo());

        context.setVariable("firstName", attendeeDTO.getFirstName());

        context.setVariable("eventDate", bookingEvent.getEventDate());
        context.setVariable("eventTime", bookingEvent.getEventTime());
        context.setVariable("bookingEventTotal", bookingEvent.getTotal());
        context.setVariable(
                "activityDetailsUrl",
                buildActivityDetailsUrl(bookingEvent.getEvent(), booking.getLanguage()));

        if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.CN) {
            context.setVariable("lang", Enums.Language.CN.name());
            context.setVariable("eventName", bookingEvent.getEvent().getNameZhCn());
            context.setVariable("title", template.getTitleZhCn());
            context.setVariable("subject", template.getSubjectZhCn());
            context.setVariable("mainBody", template.getMainBodyZhCn());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntroZhCn());
            context.setVariable("importantInfoBody", template.getImportantInfoBodyZhCn());
            context.setVariable("contactBody", template.getContactBodyZhCn());

        } else if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.HK) {
            context.setVariable("lang", Enums.Language.HK.name());
            context.setVariable("eventName", bookingEvent.getEvent().getNameZhHk());
            context.setVariable("title", template.getTitleZhHk());
            context.setVariable("subject", template.getSubjectZhHk());
            context.setVariable("mainBody", template.getMainBodyZhHk());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntroZhHk());
            context.setVariable("importantInfoBody", template.getImportantInfoBodyZhHk());
            context.setVariable("contactBody", template.getContactBodyZhHk());

        } else {
            context.setVariable("lang", Enums.Language.EN.name());
            context.setVariable("eventName", bookingEvent.getEvent().getName());
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

    public void sendBookingReminderEmail(CreateBookingRequestDTO.AttendeeDTO attendeeDTO,
                                             Bookings booking,
                                             BookingEvents bookingEvent,
                                             List<CreateBookingRequestDTO.TicketTypeDTO> ticketsDTOs,
                                             List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs) {
        Context context = new Context();

        Map<String, String> inlineImages = embedInlineImages();

        EmailTemplates template = emailTemplatesRepository.findBookingReminderEmailTemplate();

        String ticketSummary = buildTicketSummary(ticketsDTOs, booking.getLanguage());

        context.setVariable("attendees", attendeeDTOs);

        context.setVariable("ticketSummary", ticketSummary);

        context.setVariable("bookingId", booking.getRefNo());

        context.setVariable("firstName", attendeeDTO.getFirstName());

        context.setVariable("eventDate", bookingEvent.getEventDate());
        context.setVariable("eventTime", bookingEvent.getEventTime());
        context.setVariable("bookingEventTotal", bookingEvent.getTotal());
        context.setVariable(
                "activityDetailsUrl",
                buildActivityDetailsUrl(bookingEvent.getEvent(), booking.getLanguage()));

        if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.CN) {
            context.setVariable("lang", Enums.Language.CN.name());
            context.setVariable("eventName", bookingEvent.getEvent().getNameZhCn());
            context.setVariable("title", template.getTitleZhCn());
            context.setVariable("subject", template.getSubjectZhCn());
            context.setVariable("mainBody", template.getMainBodyZhCn());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntroZhCn());
            context.setVariable("importantInfoBody", template.getImportantInfoBodyZhCn());
            context.setVariable("contactBody", template.getContactBodyZhCn());

        } else if(booking.getLanguage() != null && booking.getLanguage() == Enums.Language.HK) {
            context.setVariable("lang", Enums.Language.HK.name());
            context.setVariable("eventName", bookingEvent.getEvent().getNameZhHk());
            context.setVariable("title", template.getTitleZhHk());
            context.setVariable("subject", template.getSubjectZhHk());
            context.setVariable("mainBody", template.getMainBodyZhHk());
            context.setVariable("importantInfoIntro", template.getImportantInfoIntroZhHk());
            context.setVariable("importantInfoBody", template.getImportantInfoBodyZhHk());
            context.setVariable("contactBody", template.getContactBodyZhHk());

        } else {
            context.setVariable("lang", Enums.Language.EN.name());
            context.setVariable("eventName", bookingEvent.getEvent().getName());
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


    public String buildTicketSummary(
            List<CreateBookingRequestDTO.TicketTypeDTO> ticketTypesDTOs, Enums.Language language) {
        if (ticketTypesDTOs == null || ticketTypesDTOs.isEmpty()) {
            return "No tickets selected";
        }

        return ticketTypesDTOs.stream()
                .filter(dto -> dto.getQuantity() > 0)
                .map(dto -> TicketTypeMapper.resolveName(dto, language) + " x " + dto.getQuantity())
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

            EmailLogs logs = EmailLogs.builder()
                    .userId(userId)
                    .emailParameters(emailParametersJson)
                    .templateId(templateId)
                    .status(Enums.EmailStatus.SUCCESS)
                    .build();

            emailLogsRepository.save(logs);

            auditService.record("SEND_EMAIL",
                    Bookings.class.getName(),
                    null,
                    null,
                    "Email sent successfully"
            );
        } catch (MessagingException e) {
            EmailLogs logs = EmailLogs.builder()
                    .userId(userId)
                    .emailParameters(emailParametersJson)
                    .templateId(templateId)
                    .status(Enums.EmailStatus.FAILED)
                    .build();

            emailLogsRepository.save(logs);

            auditService.record("SEND_EMAIL",
                    Bookings.class.getName(),
                    null,
                    null,
                    "Failed to send email"
            );
            throw new EmailProcessException("Failed to create and populate the email messages");
        }
    }

    private Map<String, String> embedInlineImages() {
        Map<String, String> inlineImages = new HashMap<>();
        inlineImages.put("logo", "static/images/email/logo.png");
        inlineImages.put("google", "static/images/email/google.png");
        inlineImages.put("apple", "static/images/email/apple.png");
        inlineImages.put("cat", "static/images/email/cat.png");
        inlineImages.put("fb", "static/images/email/fb.png");
        inlineImages.put("wb", "static/images/email/wb.png");
        inlineImages.put("ta", "static/images/email/ta.png");
        inlineImages.put("ig", "static/images/email/ig.png");
        inlineImages.put("yt", "static/images/email/yt.png");
        return inlineImages;
    }

    private String convertContextToJson(Context context) {
        try {
            Map<String, Object> variables = new HashMap<>();

            for (String key : context.getVariableNames()) {
                variables.put(key, context.getVariable(key));
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
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

    private String buildActivityDetailsUrl(Events event, Enums.Language language) {
        String activityDetailsUrlPrefix = buildActivityDetailsUrlPrefix(language);
        if ("#".equals(activityDetailsUrlPrefix) || event == null || event.getRefNo() == null) {
            return "#";
        }
        return activityDetailsUrlPrefix + event.getRefNo();
    }

    private String buildActivityDetailsUrlPrefix(Enums.Language language) {
        String baseUrl = appProperties.getFrontend().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return "#";
        }
        String langPath =
            switch (language != null ? language : Enums.Language.EN) {
                case CN -> "zh-CN";
                case HK -> "zh-HK";
                default -> "en";
            };
        return baseUrl.strip().replaceAll("/$", "")
            + "/"
            + langPath
            + "/activity/details?id=";
    }
}