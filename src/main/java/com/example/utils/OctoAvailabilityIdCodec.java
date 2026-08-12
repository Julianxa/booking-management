package com.example.utils;

import com.example.exception.octo.OctoException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public final class OctoAvailabilityIdCodec {
    private static final String SEP = "|";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private OctoAvailabilityIdCodec() {}

    public record AvailabilityKey(String productId, String optionId, LocalDate date, String time) {}

    public static String encode(String productId, String optionId, LocalDate date, String time) {
        String raw = productId + SEP + optionId + SEP + date + SEP + normalizeTime(time);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static AvailabilityKey decode(String availabilityId) {
        if (availabilityId == null || availabilityId.isBlank()) {
            throw OctoException.badRequest("INVALID_AVAILABILITY_ID", "availabilityId is required");
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(availabilityId), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 4);
            if (parts.length != 4) {
                throw new IllegalArgumentException("bad parts");
            }
            return new AvailabilityKey(
                    parts[0], parts[1], LocalDate.parse(parts[2]), normalizeTime(parts[3]));
        } catch (OctoException e) {
            throw e;
        } catch (Exception e) {
            throw OctoException.badRequest("INVALID_AVAILABILITY_ID", "Invalid availabilityId");
        }
    }

    public static String normalizeTime(String time) {
        if (time == null || time.isBlank()) {
            return "00:00:00";
        }
        String trimmed = time.trim();
        if (trimmed.length() == 5) {
            return trimmed + ":00";
        }
        return trimmed;
    }

    public static String toOctoLocalDateTime(LocalDate date, String time) {
        LocalTime localTime = LocalTime.parse(normalizeTime(time), TIME_FMT);
        return date.atTime(localTime).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static String toOctoLocalDateTimeEnd(LocalDate date, String time, Integer durationMinutes) {
        LocalTime start = LocalTime.parse(normalizeTime(time), TIME_FMT);
        int minutes = durationMinutes != null && durationMinutes > 0 ? durationMinutes : 60;
        return date.atTime(start).plusMinutes(minutes).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
