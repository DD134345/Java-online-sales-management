package vgu.sales.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utility class for formatting money, dates, and timestamps for display in the UI.
 * All methods are static and stateless to ensure thread safety and reusability.
 */
public final class UiFormat {
    /**
     * Formatter for dates in dd/MM/yyyy format (e.g. 24/10/2026).
     * Private: only the format methods of this class use it.
     */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /**
     * Formatter for date and time in dd/MM/yyyy HH:mm:ss format (e.g. 24/10/2026 18:42:11).
     * Private: only the format methods of this class use it.
     */
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Private constructor: utility class, never instantiated.
     */
    private UiFormat() { }

    /**
     * Formats the given amount as money in #,##0 VND form.
     * Rounds null to "", and non-null to nearest integer using HALF_UP, with thousands separator, then appends " VND".
     * New DecimalFormat is used each call for thread safety, as DecimalFormat is not thread-safe.
     *
     * @param amount the monetary amount
     * @return formatted string or empty string if amount is null
     */
    public static String money(BigDecimal amount) {
        if (amount == null) return "";
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat df = new DecimalFormat("#,##0", sym);
        return df.format(rounded) + " VND";
    }
    /**
     * Formats a LocalDate to dd/MM/yyyy, or "" if null.
     *
     * @param date the date value
     * @return formatted date string or empty string if date is null
     */
    public static String date(LocalDate date) {
        if (date == null) return "";
        return DATE.format(date);
    }
    /**
     * Formats a LocalDateTime to dd/MM/yyyy HH:mm:ss, or "" if null.
     *
     * @param value the LocalDateTime value
     * @return formatted date-time string or empty string if value is null
     */
    public static String dateTime(LocalDateTime value) {
        if (value == null) return "";
        return DATE_TIME.format(value);
    }
}
