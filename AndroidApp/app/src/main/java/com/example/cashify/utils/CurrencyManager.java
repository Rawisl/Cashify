package com.example.cashify.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class CurrencyManager {
    public static final String PREFS_NAME = "SettingsPrefs";
    public static final String KEY_SELECTED_CURRENCY = "selected_currency";
    public static final String CURRENCY_VND = "VND";
    public static final String CURRENCY_USD = "USD";
    public static final double VND_PER_USD = 26_000.0d;

    private static Context appContext;
    static final DecimalFormat VND_FORMAT;
    static final DecimalFormat VND_COMPACT_FORMAT;
    static final DecimalFormat USD_WHOLE_FORMAT;
    static final DecimalFormat USD_DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        VND_FORMAT = new DecimalFormat("#,###", symbols);
        VND_COMPACT_FORMAT = new DecimalFormat("#,##0.#", symbols);
        USD_WHOLE_FORMAT = new DecimalFormat("#,##0", symbols);
        USD_DECIMAL_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private CurrencyManager() {}

    public static void init(Context context) {
        if (context != null) appContext = context.getApplicationContext();
    }

    public static String getSelectedCurrency() {
        if (appContext == null) return CURRENCY_VND;
        return getSelectedCurrency(appContext);
    }

    public static String getSelectedCurrency(Context context) {
        if (context == null) return CURRENCY_VND;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SELECTED_CURRENCY, CURRENCY_VND);
    }

    public static void setSelectedCurrency(Context context, String currencyCode) {
        if (context == null) return;
        String safeCode = CURRENCY_USD.equals(currencyCode) ? CURRENCY_USD : CURRENCY_VND;
        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SELECTED_CURRENCY, safeCode)
                .apply();
    }

    public static boolean isUsd() {
        return CURRENCY_USD.equals(getSelectedCurrency());
    }

    public static double toDisplayAmount(double baseVndAmount) {
        return isUsd() ? baseVndAmount / VND_PER_USD : baseVndAmount;
    }

    public static double toBaseAmount(double displayAmount) {
        return isUsd() ? displayAmount * VND_PER_USD : displayAmount;
    }

    public static String formatFull(double baseVndAmount) {
        return CurrencyFormatter.formatFullAmount(baseVndAmount);
    }

    public static String formatCompact(double baseVndAmount) {
        return CurrencyFormatter.formatCompactAmount(baseVndAmount);
    }

    public static String formatInputValue(double baseVndAmount) {
        double displayAmount = Math.abs(toDisplayAmount(baseVndAmount));
        if (isUsd()) return USD_DECIMAL_FORMAT.format(displayAmount);
        return VND_FORMAT.format(Math.round(displayAmount));
    }

    public static double parseInputToBase(String input) {
        if (input == null || input.trim().isEmpty()) return 0;
        String cleaned = input.replace("$", "")
                .replace("\u20ab", "")
                .replace("VND", "")
                .replace("USD", "")
                .trim();

        try {
            if (isUsd()) {
                cleaned = cleaned.replace(",", "");
                if (cleaned.isEmpty()) return 0;
                return new BigDecimal(cleaned)
                        .multiply(BigDecimal.valueOf(VND_PER_USD))
                        .doubleValue();
            }
            cleaned = cleaned.replaceAll("[^\\d]", "");
            return cleaned.isEmpty() ? 0 : Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static String formatNumpadDigits(String digits) {
        String safeDigits = normalizeDigits(digits);
        if (isUsd()) {
            BigDecimal dollars = new BigDecimal(safeDigits)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
            return CurrencyFormatter.formatUsdUnsigned(dollars.doubleValue());
        }
        return "\u20ab" + VND_FORMAT.format(Long.parseLong(safeDigits));
    }

    public static String numpadRawToDisplayInput(String digits) {
        String safeDigits = normalizeDigits(digits);
        if (isUsd()) {
            return new BigDecimal(safeDigits)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
                    .toPlainString();
        }
        return safeDigits;
    }

    public static String numpadDigitsFromBase(double baseVndAmount) {
        if (isUsd()) {
            BigDecimal usd = BigDecimal.valueOf(Math.max(0, baseVndAmount))
                    .divide(BigDecimal.valueOf(VND_PER_USD), 2, RoundingMode.HALF_UP);
            return usd.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .toPlainString();
        }
        return String.valueOf(Math.max(0L, Math.round(baseVndAmount)));
    }

    private static String normalizeDigits(String digits) {
        if (digits == null) return "0";
        String cleaned = digits.replaceAll("[^\\d]", "");
        if (cleaned.isEmpty()) return "0";
        String trimmed = cleaned.replaceFirst("^0+(?!$)", "");
        return trimmed.isEmpty() ? "0" : trimmed;
    }
}
