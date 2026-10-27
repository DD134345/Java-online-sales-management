package vgu.sales.ui;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UiFormatTest {

    @Test
    void moneyUsesThousandsSeparatorsAndVnd() {
        assertEquals("150,000 VND", UiFormat.money(new BigDecimal("150000")), "Should use thousands separator and VND");
    }

    @Test
    void moneyRoundsToWholeDong() {
        assertEquals("2,000 VND", UiFormat.money(new BigDecimal("1999.50")), "Should round up to nearest dong");
    }

    @Test
    void nullValuesBecomeEmptyText() {
        assertEquals("", UiFormat.money(null));
        assertEquals("", UiFormat.date(null));
        assertEquals("", UiFormat.dateTime(null));
    }

    @Test
    void datesUseDayMonthYear() {
        assertEquals("20/10/2026", UiFormat.date(LocalDate.of(2026, 10, 20)));
        assertEquals("20/10/2026 09:05:07", UiFormat.dateTime(LocalDateTime.of(2026, 10, 20, 9, 5, 7)));
    }
}
