package com.example.service;

import com.example.model.entity.PasswordResetSessions;
import com.example.model.entity.PasswordResetSessions.Status;
import com.example.repository.PasswordResetSessionsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class PasswordResetSessionService {
    private static final int TOKEN_BYTES = 32;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final PasswordResetSessionsRepository sessionsRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long ttlMinutes;
    private final int maxRequestsPerWindow;

    public PasswordResetSessionService(
            PasswordResetSessionsRepository sessionsRepository,
            @Value("${app.password-reset.ttl-minutes:10}") long ttlMinutes,
            @Value("${app.password-reset.max-requests-per-window:5}") int maxRequestsPerWindow) {
        this.sessionsRepository = sessionsRepository;
        this.ttlMinutes = ttlMinutes;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    @Transactional
    public String startPending(String email) {
        String normalized = requireEmail(email);
        ZonedDateTime now = ZonedDateTime.now();
        long recent = sessionsRepository.countByEmailAndCreatedAtAfter(
                normalized, now.minusMinutes(ttlMinutes));
        if (recent >= maxRequestsPerWindow) {
            throw new IllegalArgumentException("Too many password reset requests. Please try again later.");
        }

        sessionsRepository.supersedeActiveByEmail(
                normalized,
                now,
                Status.SUPERSEDED,
                List.of(Status.PENDING_OTP, Status.OTP_VERIFIED));

        String token = newToken();
        PasswordResetSessions session = PasswordResetSessions.builder()
                .email(normalized)
                .tokenHash(sha256Hex(token))
                .status(Status.PENDING_OTP)
                .failedAttempts(0)
                .expiresAt(now.plusMinutes(ttlMinutes))
                .build();
        sessionsRepository.save(session);
        return token;
    }

    @Transactional
    public void requirePending(String email, String sessionToken) {
        requireSession(email, sessionToken, Status.PENDING_OTP);
    }

    @Transactional
    public void registerFailedOtpAttempt(String email, String sessionToken) {
        PasswordResetSessions session = requireSession(email, sessionToken, Status.PENDING_OTP);
        session.setFailedAttempts(session.getFailedAttempts() + 1);
        if (session.getFailedAttempts() >= MAX_OTP_ATTEMPTS) {
            session.setStatus(Status.CONSUMED);
            session.setConsumedAt(ZonedDateTime.now());
        }
        sessionsRepository.save(session);
        if (session.getStatus() == Status.CONSUMED) {
            throw new IllegalArgumentException("Too many invalid confirmation codes. Please start again.");
        }
    }

    @Transactional
    public String markOtpVerified(String email, String pendingSessionToken) {
        PasswordResetSessions session = requireSession(email, pendingSessionToken, Status.PENDING_OTP);
        String nextToken = newToken();
        session.setTokenHash(sha256Hex(nextToken));
        session.setStatus(Status.OTP_VERIFIED);
        session.setFailedAttempts(0);
        session.setExpiresAt(ZonedDateTime.now().plusMinutes(ttlMinutes));
        sessionsRepository.save(session);
        return nextToken;
    }

    @Transactional
    public void requireVerified(String email, String sessionToken) {
        requireSession(email, sessionToken, Status.OTP_VERIFIED);
    }

    @Transactional
    public void consumeVerifiedSession(String email, String sessionToken) {
        PasswordResetSessions session = requireSession(email, sessionToken, Status.OTP_VERIFIED);
        session.setStatus(Status.CONSUMED);
        session.setConsumedAt(ZonedDateTime.now());
        sessionsRepository.save(session);
    }

    private PasswordResetSessions requireSession(String email, String sessionToken, Status expectedStatus) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new IllegalArgumentException("Reset session is required");
        }
        String normalized = requireEmail(email);
        PasswordResetSessions session = sessionsRepository.findByTokenHash(sha256Hex(sessionToken.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Reset session is invalid or expired"));

        if (session.getExpiresAt().isBefore(ZonedDateTime.now())
                || session.getStatus() == Status.CONSUMED
                || session.getStatus() == Status.SUPERSEDED) {
            throw new IllegalArgumentException("Reset session is invalid or expired");
        }
        if (!session.getEmail().equalsIgnoreCase(normalized)) {
            throw new IllegalArgumentException("Reset session does not match the requested email");
        }
        if (session.getStatus() != expectedStatus) {
            throw new IllegalArgumentException("Reset session is not in the expected state");
        }
        return session;
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
