package com.example.utils;

import com.example.constant.Enums;
import com.example.model.dto.CreateBookingRequestDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CountryNameResolver {

    private Map<String, Map<String, String>> translationsByLanguage = Map.of();

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource("i18n/countries.json").getInputStream()) {
            Map<String, Map<String, String>> loaded =
                    new ObjectMapper().readValue(in, new TypeReference<>() {});
            translationsByLanguage = loaded != null ? loaded : Map.of();
            log.info(
                    "Loaded country name translations for languages: {}",
                    translationsByLanguage.keySet());
        } catch (Exception e) {
            log.error("Failed to load country name translations from i18n/countries.json", e);
            translationsByLanguage = Map.of();
        }
    }

    public String translate(String countryKey, Enums.Language language) {
        if (countryKey == null || countryKey.isBlank()) {
            return countryKey;
        }
        String key = countryKey.trim();
        Enums.Language lang = language != null ? language : Enums.Language.EN;
        Map<String, String> map =
                translationsByLanguage.getOrDefault(lang.name(), Collections.emptyMap());
        String translated = map.get(key);
        if (translated != null) {
            return translated;
        }
        Map<String, String> en = translationsByLanguage.getOrDefault("EN", Collections.emptyMap());
        return en.getOrDefault(key, key);
    }

    public List<CreateBookingRequestDTO.AttendeeDTO> localizeAttendees(
            List<CreateBookingRequestDTO.AttendeeDTO> attendees, Enums.Language language) {
        if (attendees == null || attendees.isEmpty()) {
            return attendees;
        }
        return attendees.stream()
                .map(attendee -> localizeAttendee(attendee, language))
                .toList();
    }

    public CreateBookingRequestDTO.AttendeeDTO localizeAttendee(
            CreateBookingRequestDTO.AttendeeDTO attendee, Enums.Language language) {
        if (attendee == null) {
            return null;
        }
        return CreateBookingRequestDTO.AttendeeDTO.builder()
                .firstName(attendee.getFirstName())
                .lastName(attendee.getLastName())
                .email(attendee.getEmail())
                .phone(attendee.getPhone())
                .gender(attendee.getGender())
                .country(translate(attendee.getCountry(), language))
                .sequence(attendee.getSequence())
                .build();
    }
}
