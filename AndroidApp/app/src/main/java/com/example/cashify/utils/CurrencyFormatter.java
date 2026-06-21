package com.example.cashify.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyFormatter {

    public static String formatCompactAmount(double baseVndAmount) {
        if (CurrencyManager.isUsd()) {
            return formatUsdSigned(CurrencyManager.toDisplayAmount(baseVndAmount));
        }

        double absAmount = Math.abs(baseVndAmount);
        String sign = baseVndAmount < 0 ? "-" : "";

        if (absAmount >= 1_000_000_000) {
            return sign + trimCompact(absAmount / 1_000_000_000) + "B";
        }
        
        return sign + CurrencyManager.VND_FORMAT.format(Math.round(absAmount)) + "\u0111";
    }

    public static String formatFullAmount(double baseVndAmount) {
        if (CurrencyManager.isUsd()) {
            return formatUsdSigned(CurrencyManager.toDisplayAmount(baseVndAmount));
        }

        double absAmount = Math.abs(baseVndAmount);
        String sign = baseVndAmount < 0 ? "-" : "";
        return sign + CurrencyManager.VND_FORMAT.format(Math.round(absAmount)) + " \u0111";
    }

    public static String formatUsdUnsigned(double usdAmount) {
        double rounded = roundUsd(Math.abs(usdAmount));
        boolean hasCents = BigDecimal.valueOf(rounded).remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0;
        return (hasCents ? CurrencyManager.USD_DECIMAL_FORMAT : CurrencyManager.USD_WHOLE_FORMAT).format(rounded) + "$";
    }

    private static String formatUsdSigned(double usdAmount) {
        double rounded = roundUsd(usdAmount);
        String sign = rounded < 0 ? "-" : "";
        return sign + formatUsdUnsigned(rounded);
    }

    private static double roundUsd(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String trimCompact(double value) {
        double rounded = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
        if (rounded == Math.rint(rounded)) {
            return String.valueOf((long) rounded);
        }
        return CurrencyManager.VND_COMPACT_FORMAT.format(rounded);
    }

    public static String formatFullVND(double amount) {
        return formatFullAmount(amount);
    }

    public static String formatCompactVND(double amount) {
        return formatCompactAmount(amount);
    }

    public static double parseVNDToDouble(String formattedAmount) {
        return CurrencyManager.parseInputToBase(formattedAmount);
    }

    public static String formatDoubleToVND(double amount) {
        return CurrencyManager.formatInputValue(amount);
    }
}