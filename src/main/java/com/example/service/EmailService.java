package com.example.service;

import com.example.config.AppProperties;
import com.example.exception.email.EmailProcessException;
import com.example.exception.email.EmailTemplateNotFoundException;
import com.example.exception.ticket.TicketTypeNotFoundException;
import com.example.mapper.EmailTemplateMapper;
import com.example.model.dto.*;
import com.example.model.entity.*;
import com.example.repository.EmailTemplatesRepository;
import com.example.repository.TicketTypesRepository;
import com.example.utils.QRCodeGenerator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.constant.Enums.UserRole.AGENT;


@Service
@RequiredArgsConstructor
public class EmailService {
    private final TemplateEngine templateEngine;
    private final EmailTemplatesRepository emailTemplatesRepository;
    private final TicketTypesRepository ticketTypesRepository;
    private final QRCodeGenerator qrCodeGenerator;
    private final JavaMailSender javaMailSender;
    private final AppProperties appProperties;
    private final EmailTemplateMapper emailTemplateMapper;
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

    public record BookingReConfirmedEvent(
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
        getEmailTemplateResponseDTO.setTimestamp(LocalDateTime.now());
        return getEmailTemplateResponseDTO;
    }

    public GetListEmailTemplatesResponseDTO getAllEmailTemplates(Pageable pageable) {
        Page<EmailTemplates> emailTemplatesPage = emailTemplatesRepository.findAllActive(pageable);

        List<GetEmailTemplateResponseDTO> content = emailTemplatesPage.getContent().stream()
                .map(emailTemplateMapper::toResponseDTO)
                .toList();

        GetListEmailTemplatesResponseDTO getListEmailTemplatesResponseDTO = emailTemplateMapper.toGetListResponse(emailTemplatesPage, content);
        getListEmailTemplatesResponseDTO.setMessage("Retrieve list of Email Templates successfully.");
        getListEmailTemplatesResponseDTO.setTimestamp(LocalDateTime.now());
        return getListEmailTemplatesResponseDTO;
    }

    @Transactional
    public UpdateEmailTemplatesResponseDTO updateEmailTemplate(String templateRefNo, UpdateEmailTemplatesRequestDTO updateEmailTemplatesRequestDTO) {

        EmailTemplates template = emailTemplatesRepository.findByRefNo(templateRefNo)
                .orElseThrow(() -> new EmailTemplateNotFoundException(String.format("Email template not found with code %s", templateRefNo)));

        if (updateEmailTemplatesRequestDTO.getSubject() != null)
            template.setSubject(updateEmailTemplatesRequestDTO.getSubject());
        if (updateEmailTemplatesRequestDTO.getMainBody() != null)
            template.setMainBody(updateEmailTemplatesRequestDTO.getMainBody());
        if (updateEmailTemplatesRequestDTO.getImportantInfoIntro() != null)
            template.setImportantInfoIntro(updateEmailTemplatesRequestDTO.getImportantInfoIntro());
        if (updateEmailTemplatesRequestDTO.getImportantInfoBody() != null)
            template.setImportantInfoBody(updateEmailTemplatesRequestDTO.getImportantInfoBody());
        if (updateEmailTemplatesRequestDTO.getContactBody() != null)
            template.setContactBody(updateEmailTemplatesRequestDTO.getContactBody());
        template = emailTemplatesRepository.save(template);

        return emailTemplateMapper.toUpdateResponseDTO(template);
    }

    @Async
    public void sendBookingOrderSummaryEmail(Users user, Bookings booking, List<CreateBookingRequestDTO.BookingEventDTO> eventList, String giftCertificatePromoCode, List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTicketList) {
        Context context = new Context();

        Map<String, String> inlineImages = embedInlineImages();

        EmailTemplates templates = emailTemplatesRepository.findBookingOrderSummaryEmailTemplate();

        context.setVariable("bookingId", booking.getRefNo());
        context.setVariable("grandTotal", booking.getTotalPaidPrice());
        context.setVariable("finalAmount", booking.getFinalPaidAmount());
        context.setVariable("currency", booking.getCurrency());

        context.setVariable("firstName", user.getFirstName());

        context.setVariable("bookingEvents", eventList);
        context.setVariable("redeemedTickets", redeemedTicketList);

        context.setVariable("giftCertificatePromoCode", giftCertificatePromoCode);
        context.setVariable("giftCertificateDiscount", booking.getDiscount());

        context.setVariable("subject", templates.getSubject());
        context.setVariable("mainBody", templates.getMainBody());
        context.setVariable("importantInfoIntro", templates.getImportantInfoIntro());
        context.setVariable("importantInfoBody", templates.getImportantInfoBody());
        context.setVariable("contactBody", templates.getContactBody());

        String template = templateEngine.process("booking-order-summary-email-template", context);

        sendEmail(user.getEmail(), "Confirm Your Payment", template, inlineImages);
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

        EmailTemplates templates = emailTemplatesRepository.findBookingConfirmationEmailTemplate();

        String ticketSummary = buildTicketSummary(ticketsDTOs);

        context.setVariable("attendees", attendeeDTOs);

        context.setVariable("ticketSummary", ticketSummary);

        context.setVariable("bookingId", booking.getRefNo());

        context.setVariable("firstName", attendeeDTO.getFirstName());

        context.setVariable("eventName", bookingEvent.getEvent().getName());
        context.setVariable("eventDate", bookingEvent.getEventDate());
        context.setVariable("eventTime", bookingEvent.getEventTime());
        context.setVariable("bookingEventTotal", bookingEvent.getTotal());

        context.setVariable("subject", templates.getSubject());
        context.setVariable("mainBody", templates.getMainBody());
        context.setVariable("importantInfoIntro", templates.getImportantInfoIntro());
        context.setVariable("importantInfoBody", templates.getImportantInfoBody());
        context.setVariable("contactBody", templates.getContactBody());

        String template = templateEngine.process("booking-confirmation-email-template", context);

        sendEmail(attendeeDTO.getEmail(), "Confirm Your Booking", template, inlineImages);
    }

    @Async
    public void sendBookingCancellationEmail(CreateBookingRequestDTO.AttendeeDTO attendeeDTO,
                                             Bookings booking,
                                             BookingEvents bookingEvent,
                                             List<CreateBookingRequestDTO.TicketTypeDTO> ticketsDTOs,
                                             List<CreateBookingRequestDTO.AttendeeDTO> attendeeDTOs) {
        Context context = new Context();

        Map<String, String> inlineImages = embedInlineImages();

        EmailTemplates templates = emailTemplatesRepository.findBookingCancellationEmailTemplate();

        String ticketSummary = buildTicketSummary(ticketsDTOs);

        context.setVariable("attendees", attendeeDTOs);

        context.setVariable("ticketSummary", ticketSummary);

        context.setVariable("bookingId", booking.getRefNo());

        context.setVariable("firstName", attendeeDTO.getFirstName());

        context.setVariable("eventName", bookingEvent.getEvent().getName());
        context.setVariable("eventDate", bookingEvent.getEventDate());
        context.setVariable("eventTime", bookingEvent.getEventTime());
        context.setVariable("bookingEventTotal", bookingEvent.getTotal());

        context.setVariable("subject", templates.getSubject());
        context.setVariable("mainBody", templates.getMainBody());
        context.setVariable("importantInfoIntro", templates.getImportantInfoIntro());
        context.setVariable("importantInfoBody", templates.getImportantInfoBody());
        context.setVariable("contactBody", templates.getContactBody());

        String template = templateEngine.process("booking-cancellation-email-template", context);

        sendEmail(attendeeDTO.getEmail(), "Cancel Your Booking", template, inlineImages);
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

    private void sendEmail(String to, String subject, String htmlContent, Map<String, String> inlineImages) {
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
        } catch (MessagingException e) {
            throw new EmailProcessException("Failed to create and populate the email messages");
        }
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
}