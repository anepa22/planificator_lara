package ar.com.anepanet.planificator.service;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VacationQuotaTest {

    @Test
    void scaleBreaksAtFiveSixTenElevenTwentyAndTwentyOneYears() {
        assertEquals(14, legalAt(LocalDate.of(2021, 12, 31), 2026));
        assertEquals(21, legalAt(LocalDate.of(2020, 12, 31), 2026));
        assertEquals(21, legalAt(LocalDate.of(2016, 12, 31), 2026));
        assertEquals(28, legalAt(LocalDate.of(2015, 12, 31), 2026));
        assertEquals(28, legalAt(LocalDate.of(2006, 12, 31), 2026));
        assertEquals(35, legalAt(LocalDate.of(2005, 12, 31), 2026));
    }

    @Test
    void julyFirstWhoWorksThroughDecemberKeepsTheFullScale() {
        LocalDate hire = LocalDate.of(2026, 7, 1);
        int worked = workedMondayToSaturday(hire, LocalDate.of(2026, 12, 31));
        assertTrue(worked * 2L >= VacationQuota.businessDays(2026));
        assertEquals(14, VacationQuota.legalDays(hire, 2026, worked));
    }

    @Test
    void nineteenWorkedDaysAreZeroAndFortyAreTwo() {
        LocalDate hire = LocalDate.of(2026, 11, 1);
        assertEquals(0, VacationQuota.legalDays(hire, 2026, 19));
        assertEquals(2, VacationQuota.legalDays(hire, 2026, 40));
        assertEquals(0, VacationQuota.legalDays(LocalDate.of(2021, 12, 31), 2026, 19));
    }

    @Test
    void octoberSecondAndSeptemberTwentiethBelongToThePeriodStartingOctober2026() {
        assertEquals(2026, VacationQuota.periodYear(LocalDate.of(2026, 10, 2)));
        assertEquals(2026, VacationQuota.periodYear(LocalDate.of(2027, 9, 20)));
        assertEquals(LocalDate.of(2026, 10, 1), VacationQuota.periodStart(2026));
        assertEquals(LocalDate.of(2027, 9, 30), VacationQuota.periodEnd(2026));
    }

    @Test
    void unusedDaysCarryIntoTheNextPeriod() {
        LocalDate hire = LocalDate.of(2026, 1, 1);
        List<VacationQuota.PeriodBalance> rows = VacationQuota.balances(
                hire,
                2026,
                2027,
                Map.of(2026, 5),
                Map.of(2026, 10),
                Map.of(2026, VacationQuota.businessDays(2026), 2027, VacationQuota.businessDays(2027)));

        VacationQuota.PeriodBalance current = row(rows, 2026);
        assertEquals(14, current.legalDays());
        assertEquals(5, current.agreedDays());
        assertEquals(10, current.takenDays());
        assertEquals(9, current.remainingDays());
        assertEquals(9, row(rows, 2027).accumulatedDays());
    }

    @Test
    void balanceShowsLawAgreedCarryTakenAndRemaining() {
        LocalDate hire = LocalDate.of(2025, 1, 1);
        List<VacationQuota.PeriodBalance> rows = VacationQuota.balances(
                hire,
                2026,
                2026,
                Map.of(2026, 5),
                Map.of(2025, 10, 2026, 10),
                Map.of(
                        2025, VacationQuota.businessDays(2025),
                        2026, VacationQuota.businessDays(2026)));

        VacationQuota.PeriodBalance row = row(rows, 2026);
        assertEquals(14, row.legalDays());
        assertEquals(5, row.agreedDays());
        assertEquals(4, row.accumulatedDays());
        assertEquals(10, row.takenDays());
        assertEquals(23, row.usableDays());
        assertEquals(13, row.remainingDays());
    }

    @Test
    void rejectsWhenTheNewDaysExceedTheRemainingQuota() {
        LocalDate hire = LocalDate.of(2026, 1, 1);
        Set<LocalDate> taken = new HashSet<>();
        for (int day = 1; day <= 10; day++) {
            taken.add(LocalDate.of(2026, 11, day));
        }
        List<LocalDate> proposed = VacationQuota.eachDate(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5));

        String message = VacationQuota.rejection(
                "Ana",
                hire,
                proposed,
                Map.of(),
                Map.of(2026, taken),
                Map.of(2026, VacationQuota.businessDays(2026)));

        assertEquals("A Ana le quedan 4 días en el período del 1 oct 2026 – 30 sep 2027.", message);
    }

    @Test
    void rejectsThePeriodThatCrossesOctoberWithoutTouchingTheOther() {
        LocalDate hire = LocalDate.of(2026, 1, 1);
        Set<LocalDate> taken = new HashSet<>();
        for (int day = 1; day <= 12; day++) {
            taken.add(LocalDate.of(2026, 11, day));
        }
        List<LocalDate> proposed = VacationQuota.eachDate(
                LocalDate.of(2027, 9, 28), LocalDate.of(2027, 10, 2));
        Map<Integer, Integer> worked = Map.of(
                2026, VacationQuota.businessDays(2026),
                2027, VacationQuota.businessDays(2027));

        String message = VacationQuota.rejection(
                "Ana", hire, proposed, Map.of(), Map.of(2026, taken), worked);

        assertEquals("A Ana le quedan 2 días en el período del 1 oct 2026 – 30 sep 2027.", message);
    }

    @Test
    void acceptsARangeThatFitsEachPeriod() {
        LocalDate hire = LocalDate.of(2026, 1, 1);
        List<LocalDate> proposed = VacationQuota.eachDate(
                LocalDate.of(2027, 9, 28), LocalDate.of(2027, 10, 2));
        String message = VacationQuota.rejection(
                "Ana",
                hire,
                proposed,
                Map.of(),
                Map.of(),
                Map.of(2026, VacationQuota.businessDays(2026), 2027, VacationQuota.businessDays(2027)));
        assertNull(message);
    }

    @Test
    void missingHireDateRejectsBeforeAnyQuotaMath() {
        String message = VacationQuota.rejection(
                "Ana", null, List.of(LocalDate.of(2026, 10, 2)), Map.of(), Map.of(), Map.of());
        assertEquals("Falta la fecha de ingreso de Ana.", message);
    }

    private static int legalAt(LocalDate hireDate, int periodYear) {
        return VacationQuota.legalDays(hireDate, periodYear, VacationQuota.businessDays(periodYear));
    }

    private static int workedMondayToSaturday(LocalDate from, LocalDate to) {
        int count = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    private static VacationQuota.PeriodBalance row(List<VacationQuota.PeriodBalance> rows, int year) {
        return rows.stream()
                .filter(row -> row.periodYear() == year)
                .findFirst()
                .orElseThrow();
    }
}
