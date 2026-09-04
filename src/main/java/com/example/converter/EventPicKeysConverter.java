package com.example.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Converter
public class EventPicKeysConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            List<String> cleaned = attribute.stream().filter(k -> k != null && !k.isBlank()).toList();
            if (cleaned.isEmpty()) {
                return null;
            }
            return MAPPER.writeValueAsString(cleaned);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize event pic keys", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        String trimmed = dbData.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = MAPPER.readValue(trimmed, LIST_TYPE);
                return parsed == null ? new ArrayList<>() : new ArrayList<>(parsed);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to deserialize event pic keys", e);
            }
        }
        return new ArrayList<>(Collections.singletonList(trimmed));
    }
}
