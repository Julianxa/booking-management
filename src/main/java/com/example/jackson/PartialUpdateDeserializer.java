package com.example.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PartialUpdateDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private final JavaType javaType;

    public PartialUpdateDeserializer() {
        this.javaType = null;
    }

    private PartialUpdateDeserializer(JavaType javaType) {
        this.javaType = javaType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType type = property != null ? property.getType() : ctxt.getContextualType();
        return new PartialUpdateDeserializer(type);
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        JsonNode node = ctxt.readTree(parser);
        Class<?> rawClass = javaType.getRawClass();

        Object bean;
        try {
            bean = rawClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw ctxt.instantiationException(rawClass, e);
        }

        Set<String> presentFields = new HashSet<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String jsonName = entry.getKey();
            presentFields.add(jsonName);

            Field field = findField(rawClass, jsonName);
            if (field == null) {
                continue;
            }

            field.setAccessible(true);
            JavaType fieldType = ctxt.getTypeFactory().constructType(field.getGenericType());
            Object value = entry.getValue().isNull()
                    ? null
                    : ctxt.readTreeAsValue(entry.getValue(), fieldType);
            try {
                field.set(bean, value);
            } catch (IllegalAccessException e) {
                throw ctxt.instantiationException(rawClass, e);
            }
        }

        if (bean instanceof AbstractPartialUpdateDto partialUpdateDto) {
            partialUpdateDto.setPresentFields(presentFields);
        }

        return bean;
    }

    private static Field findField(Class<?> clazz, String jsonPropertyName) {
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                String name = jsonProperty != null && !jsonProperty.value().isBlank()
                        ? jsonProperty.value()
                        : field.getName();
                if (name.equals(jsonPropertyName)) {
                    return field;
                }
            }
        }
        return null;
    }
}
