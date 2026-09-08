package com.example.service;

import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetSessionService {
    private static final ZoneId HKT = ZoneId.of("Asia/Hong_Kong");
    private static final long SESSION_TTL_MINUTES = 10;

    private final Map<String, ResetSession> sessions = new ConcurrentHashMap<>();

    public String startPending(String email) {
        String normalized = requireEmail(email);
        pruneExpired();
        String session = UUID.randomUUID().toString();
        sessions.put(
                session,
                new ResetSession(
                        normalized,
                        ResetSessionStatus.PENDING_OTP,
                        ZonedDateTime.now(HKT).plusMinutes(SESSION_TTL_MINUTES)));
        return session;
    }

    public void requirePending(String email, String session) {
        requireSession(email, session, ResetSessionStatus.PENDING_OTP);
    }

    public String markOtpVerified(String email, String pendingSession) {
        String normalized = requireEmail(email);
        requireSession(normalized, pendingSession, ResetSessionStatus.PENDING_OTP);
        sessions.put(
                pendingSession,
                new ResetSession(
                        normalized,
                        ResetSessionStatus.OTP_VERIFIED,
                        ZonedDateTime.now(HKT).plusMinutes(SESSION_TTL_MINUTES)));
        return pendingSession;
    }

    public void consumeVerifiedSession(String email, String session) {
        requireSession(email, session, ResetSessionStatus.OTP_VERIFIED);
        sessions.remove(session);
    }

    private ResetSession requireSession(String email, String session, ResetSessionStatus expectedStatus) {
        pruneExpired();
        if (session == null || session.isBlank()) {
            throw new IllegalArgumentException("Reset session is required");
        }
        ResetSession resetSession = sessions.get(session);
        if (resetSession == null || resetSession.expiresAt().isBefore(ZonedDateTime.now(HKT))) {
            sessions.remove(session);
            throw new IllegalArgumentException("Reset session is invalid or expired");
        }
        if (!resetSession.email().equalsIgnoreCase(requireEmail(email))) {
            throw new IllegalArgumentException("Reset session does not match the requested email");
        }
        if (resetSession.status() != expectedStatus) {
            throw new IllegalArgumentException("Reset session is not in the expected state");
        }
        return resetSession;
    }

    private static String requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim();
    }

    private void pruneExpired() {
        ZonedDateTime now = ZonedDateTime.now(HKT);
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private enum ResetSessionStatus {
        PENDING_OTP,
        OTP_VERIFIED
    }

    private record ResetSession(String email, ResetSessionStatus status, ZonedDateTime expiresAt) {}
}
