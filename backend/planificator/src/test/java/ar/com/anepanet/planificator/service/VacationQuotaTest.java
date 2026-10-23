package ar.com.anepanet.planificator.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VacationQuotaTest {

    @Test
    void periodFollowsOctober() {
        assertEquals(2026, VacationQuota.periodYear(LocalDate.of(2026, 10, 15)));
        assertEquals(2026, VacationQuota.periodYear(LocalDate.of(2027, 9, 30)));
        assertEquals(2027, VacationQuota.periodYear(LocalDate.of(2027, 10, 1)));
        assertTrue(VacationQuota.isEnjoymentPeriod(
                LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30)));
        assertTrue(!VacationQuota.isEnjoymentPeriod(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
    }

    @Test
    void scaleBoundaries() {
        assertEquals(14, VacationQuota.seniorityDays(LocalDate.of(2021, 12, 31), 2026));
        assertEquals(21, VacationQuota.seniorityDays(LocalDate.of(2021, 12, 30), 2026));
        assertEquals(21, VacationQuota.seniorityDays(LocalDate.of(2016, 12, 31), 2026));
        assertEquals(28, VacationQuota.seniorityDays(LocalDate.of(2016, 12, 30), 2026));
        assertEquals(28, VacationQuota.seniorityDays(LocalDate.of(2006, 12, 31), 2026));
        assertEquals(35, VacationQuota.seniorityDays(LocalDate.of(2006, 12, 30), 2026));
    }

    @Test
    void sixMonthsUsesTheScale() {
        assertEquals(14, VacationQuota.seniorityDays(LocalDate.of(2026, 6, 30), 2026));
        LocalDate endOfYear = LocalDate.of(2026, 12, 31);
        assertEquals(6, VacationQuota.seniorityDays(LocalDate.of(2026, 7, 1), 2026, endOfYear));
    }

    @Test
    void proportionalCountsWeekdaysAlreadyElapsed() {
        LocalDate hire = LocalDate.of(2026, 10, 1);
        assertEquals(17, VacationQuota.weekdays(hire, LocalDate.of(2026, 10, 23)));
        assertEquals(0, VacationQuota.seniorityDays(hire, 2026, LocalDate.of(2026, 10, 23)));
        assertEquals(0, VacationQuota.seniorityDays(hire, 2026, LocalDate.of(2026, 10, 27)));
        assertEquals(1, VacationQuota.seniorityDays(hire, 2026, LocalDate.of(2026, 10, 28)));
        assertEquals(3, VacationQuota.seniorityDays(hire, 2026, LocalDate.of(2026, 12, 31)));
        assertEquals(0, VacationQuota.seniorityDays(LocalDate.of(2026, 12, 12), 2026, LocalDate.of(2026, 12, 31)));
        assertEquals(0, VacationQuota.seniorityDays(LocalDate.of(2026, 12, 20), 2026, LocalDate.of(2026, 12, 31)));
    }

    @Test
    void missingAdjustmentIsZeroAndDoesNotRepeatCarry() {
        LocalDate hire = LocalDate.of(2024, 6, 1);
        VacationQuota.Snapshot plain = VacationQuota.snapshot(hire, 2026, Map.of(), Map.of());
        assertEquals(14, plain.thisPeriod());
        assertEquals(0, plain.carried());
        assertEquals(0, plain.extra());
        assertEquals(14, plain.remaining());

        VacationQuota.Snapshot opening = VacationQuota.snapshot(hire, 2026, Map.of(), Map.of(2026, 3));
        assertEquals(17, opening.remaining());

        VacationQuota.Snapshot both = VacationQuota.snapshot(hire, 2026, Map.of(2025, 11), Map.of(2026, 3));
        assertEquals(0, both.carried());
        assertEquals(3, both.extra());
        assertEquals(17, both.remaining());
    }

    @Test
    void enjoymentStartsOnOctober2026AndThenCarriesForward() {
        LocalDate hire = LocalDate.of(2020, 12, 30);
        VacationQuota.Snapshot now = VacationQuota.snapshot(hire, 2026, Map.of(2026, 7, 2025, 21, 2024, 21), Map.of());
        assertEquals(21, now.thisPeriod());
        assertEquals(0, now.carried());
        assertEquals(7, now.enjoyed());
        assertEquals(14, now.remaining());

        VacationQuota.Snapshot older = VacationQuota.snapshot(LocalDate.of(2022, 5, 1), 2026, Map.of(), Map.of());
        assertEquals(0, older.carried());
        assertEquals(14, older.remaining());

        VacationQuota.Snapshot next = VacationQuota.snapshot(hire, 2027, Map.of(2026, 7), Map.of());
        assertEquals(14, next.carried());
        assertEquals(VacationQuota.seniorityDays(hire, 2027) + 14, next.remaining());
    }

    @Test
    void francoDoesNotReduceTheBalance() {
        VacationQuota.Snapshot snap = VacationQuota.snapshot(
                LocalDate.of(2024, 6, 1), 2026, Map.of(), Map.of());
        assertEquals(14, snap.remaining());
    }

    @Test
    void rejectsWhenTheBalanceWouldGoNegativeOrTheQuotaIsZero() {
        LocalDate hire = LocalDate.of(2024, 6, 1);
        Map<Integer, Integer> enjoyed = Map.of(2026, 9);
        List<LocalDate> five = VacationQuota.eachDate(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));
        assertNull(VacationQuota.rejection("Ana", hire, enjoyed, Map.of(), five));
        VacationQuota.Snapshot after = VacationQuota.snapshot(hire, 2026, enjoyed, Map.of());
        assertEquals(0, after.remaining() - five.size());

        Map<Integer, Integer> tight = Map.of(2026, 12);
        List<LocalDate> three = VacationQuota.eachDate(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7));
        assertTrue(VacationQuota.rejection("Ana", hire, tight, Map.of(), three).contains("no tiene saldo"));

        String zero = VacationQuota.rejection(
                "Ana",
                LocalDate.of(2026, 12, 20),
                Map.of(),
                Map.of(),
                List.of(LocalDate.of(2026, 12, 21)));
        assertTrue(zero.contains("no tiene días"));

        assertNull(VacationQuota.rejection(
                "Ana",
                LocalDate.of(2026, 6, 1),
                Map.of(),
                Map.of(),
                List.of(LocalDate.of(2026, 10, 5))));

        assertNull(VacationQuota.rejection(
                "Ana",
                LocalDate.of(2026, 12, 20),
                Map.of(),
                Map.of(2026, 1),
                List.of(LocalDate.of(2026, 12, 21))));
    }

    @Test
    void replacingAVacationDayDoesNotCountTwiceAndACrossingRangeSplits() {
        List<LocalDate> proposed = VacationQuota.eachDate(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6));
        List<LocalDate> fresh = VacationQuota.newDates(proposed, List.of(LocalDate.of(2026, 10, 5)));
        assertEquals(List.of(LocalDate.of(2026, 10, 6)), fresh);
        assertNull(VacationQuota.rejection(
                "Ana",
                LocalDate.of(2024, 6, 1),
                Map.of(2026, 13),
                Map.of(),
                fresh));

        List<LocalDate> crossing = List.of(LocalDate.of(2027, 9, 30), LocalDate.of(2027, 10, 1));
        String message = VacationQuota.rejection(
                "Ana",
                LocalDate.of(2024, 6, 1),
                Map.of(2026, 13, 2027, 14),
                Map.of(),
                crossing);
        assertTrue(message.contains("1 oct 2027"));
    }

    @Test
    void negativeAdjustmentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> VacationQuota.requireAdjustment(-1));
    }

}
