package com.mahal.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utility helpers for formatting dates/times and currency
 * consistently across the application.
 *
 * - All times are based on Indian Standard Time (Asia/Kolkata)
 * - All money is formatted as INR with the rupee symbol.
 */
public class FormatUtil {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final Locale INDIA_LOCALE = new Locale("en", "IN");
    private static final NumberFormat INR_FORMAT =
            NumberFormat.getCurrencyInstance(INDIA_LOCALE);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private FormatUtil() {
        // Utility class
    }

    /**
     * Returns the current date in IST.
     */
    public static LocalDate todayIst() {
        return ZonedDateTime.now(IST_ZONE).toLocalDate();
    }

    /**
     * Formats a LocalDate in a user‑friendly way (IST-based).
     */
    public static String formatDate(LocalDate date) {
        if (date == null) return "-";
        return date.format(DATE_FORMAT);
    }

    /**
     * Formats a BigDecimal amount as INR, e.g. ₹12,450.00
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }
        synchronized (INR_FORMAT) {
            return INR_FORMAT.format(amount);
        }
    }
}


