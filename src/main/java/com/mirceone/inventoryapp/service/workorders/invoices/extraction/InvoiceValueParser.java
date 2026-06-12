package com.mirceone.inventoryapp.service.workorders.invoices.extraction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Lenient conversion of free-form LLM string values into typed amounts and dates. Returns null
 * (rather than throwing) on unparseable input so a single malformed field does not fail the whole
 * extraction.
 */
public final class InvoiceValueParser {

    private InvoiceValueParser() {
    }

    public static BigDecimal money(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.strip();
        if (s.isEmpty()) {
            return null;
        }
        // Drop everything except digits, separators and sign.
        s = s.replaceAll("[^0-9,.-]", "");
        if (s.isBlank() || s.equals("-") || s.equals(".") || s.equals(",")) {
            return null;
        }

        boolean hasComma = s.indexOf(',') >= 0;
        boolean hasDot = s.indexOf('.') >= 0;
        if (hasComma && hasDot) {
            // The right-most separator is the decimal point; the other groups thousands.
            if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                s = s.replace(".", "").replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (hasComma) {
            // Comma as decimal separator (e.g. European "1234,56").
            s = s.replace(',', '.');
        }

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static LocalDate date(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.strip());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String trimToNull(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        String s = raw.strip();
        if (s.isEmpty()) {
            return null;
        }
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }
}
