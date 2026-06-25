package com.example.utils;

import com.example.jackson.PartialUpdateSupport;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PartialUpdateUtil {

    private PartialUpdateUtil() {
    }

    public static <T> void apply(
            PartialUpdateSupport dto,
            String jsonProperty,
            Supplier<T> getter,
            Consumer<T> setter) {
        if (dto.hasField(jsonProperty)) {
            setter.accept(getter.get());
        }
    }

    public static void ifPresent(PartialUpdateSupport dto, String jsonProperty, Runnable action) {
        if (dto.hasField(jsonProperty)) {
            action.run();
        }
    }
}
