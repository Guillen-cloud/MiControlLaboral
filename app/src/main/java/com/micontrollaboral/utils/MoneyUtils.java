package com.micontrollaboral.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {
    private MoneyUtils() {
    }

    public static int bolivianosToCents(String value) {
        BigDecimal bolivianos = new BigDecimal(value.trim());
        BigDecimal cents = bolivianos.movePointRight(2);
        if (cents.scale() > 0 && cents.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("El importe no puede tener más de dos decimales.");
        }
        return cents.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
    }
}