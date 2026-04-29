package com.example.utils;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DataUtils {
    public BigDecimal calculatePercentage(Integer numerator, Integer denominator) {
        if (numerator == null || numerator <= 0 || denominator == null || denominator <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
