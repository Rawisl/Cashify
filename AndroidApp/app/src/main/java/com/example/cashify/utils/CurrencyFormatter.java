package com.example.cashify.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyFormatter {

    // Formats amount with K/M/B suffixes (e.g., 1.5M, 500K)
    public static String formatCompactAmount(double baseVndAmount) {
        if (CurrencyManager.isUsd()) {
            return formatUsdSigned(CurrencyManager.toDisplayAmount(baseVndAmount));
        }

        double absAmount = Math.abs(baseVndAmount);
        String sign = baseVndAmount < 0 ? "-" : "";

        if (absAmount >= 1_000_000_000) {
            return sign + trimCompact(absAmount / 1_000_000_000) + "B";
        }
        if (absAmount >= 1_000_000) {
            return sign + trimCompact(absAmount / 1_000_000) + "M";
        }
        if (absAmount >= 100_000) {
            return sign + Math.round(absAmount / 1_000) + "K";
        }
        return sign + CurrencyManager.VND_FORMAT.format(Math.round(absAmount)) + "\u0111"; // \u0111 is 'đ'
    }

    // Formats full exact amount (e.g., 1,500,000 đ)
    public static String formatFullAmount(double baseVndAmount) {
        if (CurrencyManager.isUsd()) {
            return formatUsdSigned(CurrencyManager.toDisplayAmount(baseVndAmount));
        }

        double absAmount = Math.abs(baseVndAmount);
        String sign = baseVndAmount < 0 ? "-" : "";
        return sign + CurrencyManager.VND_FORMAT.format(Math.round(absAmount)) + " \u0111";
    }

    // Formats USD without sign
    public static String formatUsdUnsigned(double usdAmount) {
        double rounded = roundUsd(Math.abs(usdAmount));
        boolean hasCents = BigDecimal.valueOf(rounded).remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0;
        return (hasCents ? CurrencyManager.USD_DECIMAL_FORMAT : CurrencyManager.USD_WHOLE_FORMAT).format(rounded) + "$";
    }

    // Formats USD with negative sign if applicable
    private static String formatUsdSigned(double usdAmount) {
        double rounded = roundUsd(usdAmount);
        String sign = rounded < 0 ? "-" : "";
        return sign + formatUsdUnsigned(rounded);
    }

    // Ensures USD is always rounded to 2 decimal places
    private static double roundUsd(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // Removes unnecessary decimal zero for compact formats (e.g., 1.0M -> 1M)
    private static String trimCompact(double value) {
        double rounded = BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
        if (rounded == Math.rint(rounded)) {
            return String.valueOf((long) rounded);
        }
        return CurrencyManager.VND_COMPACT_FORMAT.format(rounded);
    }

    // --- Legacy proxy methods to maintain backward compatibility ---
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