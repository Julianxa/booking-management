package com.example.utils;

import com.example.constant.Enums;
import com.example.exception.general.RandomReferenceNoException;
import com.example.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ReferenceNoGenerator {
    private final BookingsRepository bookingsRepository;
    private final EventsRepository eventsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final UsersRepository usersRepository;
    private final PaymentsRepository paymentsRepository;
    private final GiftCertificatesRepository giftCertificatesRepository;
    private final OrganizationsRepository organizationsRepository;
    private final RefundsRepository refundsRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SAFE_CHARS = "2346789ACDEFGHJKLMPQRTUVXY";
    private final TicketTypesRepository ticketTypesRepository;

    public String generateUserReference(Enums.UserRole role) {
        return switch (role) {
            case ADMIN -> generateUniqueReference("AD-", 10, usersRepository);
            case EMPLOYEE -> generateUniqueReference("E-", 10, usersRepository);
            case AGENT -> generateUniqueReference("A-", 10, usersRepository);
            default -> generateUniqueReference("U-", 10, usersRepository);
        };
    }

    public String generateRefundReference() {
        return generateUniqueReference("R-", 10, refundsRepository);
    }

    public String generateOrganizationReference() {
        return generateUniqueReference("O-", 10, organizationsRepository);
    }

    public String generateTicketTypeReference() {
        return generateUniqueReference("TT-", 10, ticketTypesRepository);
    }

    public String generateBookingEventReference() {
        return generateUniqueReference("BE-", 10, bookingEventsRepository);
    }

    public String generateBookingReference() {
        String datePart = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return generateUniqueReference("BKG-" + datePart + "-", 10, bookingsRepository);
    }

    public String generateEventReference() {
        return generateUniqueReference("EVT-", 10, eventsRepository);
    }

    public String generatePaymentReference() {
        return generateUniqueReference("P-", 10, paymentsRepository);
    }

    public String generateGiftCertificateReference() {
        return generateUniqueReference("GC-", 10, giftCertificatesRepository);
    }

    private String generateUniqueReference(String prefix, int randomLength, Object repository) {
        final int MAX_ATTEMPTS = 10;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String randomPart = generateRandomString(randomLength);
            String referenceNo = prefix + randomPart;

            boolean exists = false;
            if (repository instanceof BookingsRepository) {
                exists = bookingsRepository.existsByRefNo(referenceNo);
            } else if (repository instanceof BookingEventsRepository) {
                exists = bookingEventsRepository.existsByRefNo(referenceNo);
            } else if (repository instanceof UsersRepository) {
                exists = usersRepository.existsByRefNo(referenceNo);
            } else if (repository instanceof OrganizationsRepository) {
                exists = organizationsRepository.existsByRefNo(referenceNo);
            } else if (repository instanceof TicketTypesRepository) {
                exists = ticketTypesRepository.existsByRefNo(referenceNo);
            } else if (repository instanceof GiftCertificatesRepository) {
                exists = giftCertificatesRepository.existsByRefNo(referenceNo);
            }
            if (!exists) {
                return referenceNo;
            }

            if (attempt > 3) {
                System.out.println("Warning: Reference collision on attempt " + attempt + " for table ");
            }
        }

        throw new RandomReferenceNoException("Failed to generate unique reference number after " + MAX_ATTEMPTS + " attempts.");
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(SAFE_CHARS.length());
            sb.append(SAFE_CHARS.charAt(index));
        }
        return sb.toString();
    }
}
