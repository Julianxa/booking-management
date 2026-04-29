package com.example.service;

import com.example.config.AppProperties;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.EmailTemplateMapper;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.dto.UpdateEmailTemplatesRequestDTO;
import com.example.model.dto.UpdateEmailTemplatesResponseDTO;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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

    @Transactional
    public UpdateEmailTemplatesResponseDTO updateEmailTemplate(String templateRefNo, UpdateEmailTemplatesRequestDTO updateEmailTemplatesRequestDTO) {

        EmailTemplates template = emailTemplatesRepository.findByRefNo(templateRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found with code: " + templateRefNo));

        if(updateEmailTemplatesRequestDTO.getSubject() != null) template.setSubject(updateEmailTemplatesRequestDTO.getSubject());
        if(updateEmailTemplatesRequestDTO.getMainBody() != null) template.setSubject(updateEmailTemplatesRequestDTO.getMainBody());
        if(updateEmailTemplatesRequestDTO.getImportantInfoIntro() != null) template.setSubject(updateEmailTemplatesRequestDTO.getImportantInfoIntro());
        if(updateEmailTemplatesRequestDTO.getImportantInfoBody() != null) template.setSubject(updateEmailTemplatesRequestDTO.getImportantInfoBody());
        if(updateEmailTemplatesRequestDTO.getContactBody() != null) template.setSubject(updateEmailTemplatesRequestDTO.getContactBody());
        template = emailTemplatesRepository.save(template);

        return emailTemplateMapper.toUpdateResponseDto(template);
    }

    @Async
    public void sendBookingOrderSummaryEmail(Users user, Bookings booking, List<CreateBookingRequestDTO.BookingEventDTO> eventList, String giftCertificatePromoCode, List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTicketList) throws MessagingException {
        Context context = new Context();

        Map<String, String> inlineImages = embedInlineImages();

        EmailTemplates templates = emailTemplatesRepository.findBookingOrderSummaryEmailTemplate();

        context.setVariable("bookingId", booking.getRefNo());
        context.setVariable("grandTotal", booking.getTotalPaidPrice());
        context.setVariable("finalAmount", booking.getFinalPaidAmount());

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
    public void sendBookingConfirmationEmail(CreateBookingRequestDTO.AttendeeDTO attendeeDto,
                                             Bookings booking,
                                             BookingEvents bookingEvent,
                                             List<CreateBookingRequestDTO.TicketTypeDTO> ticketsDtos,
                                             List<CreateBookingRequestDTO.AttendeeDTO> attendeeDtos) throws MessagingException {
        Context context = new Context();
        String checkInUrl = appProperties.getBaseUrl()
                + appProperties.getCheckin().getPath()
                + bookingEvent.getVerificationToken();

        Map<String, String> inlineImages = embedInlineImages();
        inlineImages.put("qr", checkInUrl);

        EmailTemplates templates = emailTemplatesRepository.findBookingConfirmationEmailTemplate();

        String ticketSummary = buildTicketSummary(ticketsDtos);

        context.setVariable("attendees", attendeeDtos);

        context.setVariable("ticketSummary", ticketSummary);

        context.setVariable("bookingId", booking.getRefNo());

        context.setVariable("firstName", attendeeDto.getFirstName());

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

        sendEmail(attendeeDto.getEmail(), "Confirm Your Booking", template, inlineImages);
    }

    public String buildTicketSummary(List<CreateBookingRequestDTO.TicketTypeDTO> ticketTypesDto) {
        if (ticketTypesDto == null || ticketTypesDto.isEmpty()) {
            return "No tickets selected";
        }

        return ticketTypesDto.stream()
                .filter(dto -> dto.getQuantity() > 0)
                .map(dto -> {

                    TicketTypes ticketType = ticketTypesRepository.findByRefNo(dto.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Ticket Type not found: " + dto.getId()));
                    String name = ticketType.getName();
                    return name + " x " + dto.getQuantity();
                })
                .collect(Collectors.joining(", "));
    }

    private void sendEmail(String to, String subject, String htmlContent, Map<String, String> inlineImages) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setText(htmlContent, true);
        helper.setTo(to);
        helper.setFrom(senderEmail);
        helper.setSubject(subject);
        for (Map.Entry<String, String> entry : inlineImages.entrySet()) {
            if(entry.getKey().equals("qr")) {
                byte[] qrBytes = qrCodeGenerator.generateQrCode(entry.getValue());
                ByteArrayResource qrResource = new ByteArrayResource(qrBytes);

                helper.addInline(entry.getKey(), qrResource, "image/png");
            }
            else
                helper.addInline(entry.getKey(), new ClassPathResource(entry.getValue()));
        }
        javaMailSender.send(message);
    }

    private Map<String, String> embedInlineImages() {
        Map<String, String> inlineImages = new HashMap<>();
        inlineImages.put("logo","images/email/logo.png");
        inlineImages.put("google","images/email/google.png");
        inlineImages.put("apple","images/email/apple.png");
        inlineImages.put("cat","images/email/cat.png");
        inlineImages.put("fb","images/email/fb.png");
        inlineImages.put("wb","images/email/wb.png");
        inlineImages.put("ta","images/email/ta.png");
        inlineImages.put("ig","images/email/ig.png");
        inlineImages.put("yt","images/email/yt.png");
        return inlineImages;
    }
}