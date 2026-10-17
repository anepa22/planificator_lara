package ar.com.anepanet.planificator.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cupo de vacaciones. El período {@code Y} va del 1 de octubre de {@code Y}
 * al 30 de septiembre de {@code Y+1}. La antigüedad y la mitad de los días
 * hábiles se miden en el año calendario {@code Y}.
 */
public final class VacationQuota {

    private VacationQuota() {}

    public record PeriodBalance(
            int periodYear,
            int legalDays,
            int agreedDays,
            int accumulatedDays,
            int takenDays,
            int usableDays,
            int remainingDays
    ) {}

    public static int periodYear(LocalDate date) {
        return date.getMonthValue() >= 10 ? date.getYear() : date.getYear() - 1;
    }

    public static LocalDate periodStart(int periodYear) {
        return LocalDate.of(periodYear, 10, 1);
    }

    public static LocalDate periodEnd(int periodYear) {
        return LocalDate.of(periodYear + 1, 9, 30);
    }

    public static String periodLabel(int periodYear) {
        return "1 oct " + periodYear + " – 30 sep " + (periodYear + 1);
    }

    public static int businessDays(int calendarYear) {
        int count = 0;
        LocalDate date = LocalDate.of(calendarYear, 1, 1);
        LocalDate end = LocalDate.of(calendarYear, 12, 31);
        while (!date.isAfter(end)) {
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }

    public static int legalDays(LocalDate hireDate, int periodYear, int workedDays) {
        LocalDate cutoff = LocalDate.of(periodYear, 12, 31);
        if (hireDate.isAfter(cutoff)) {
            return 0;
        }
        int safeWorked = Math.max(0, workedDays);
        if (safeWorked * 2L < businessDays(periodYear)) {
            return safeWorked / 20;
        }
        return scale(Period.between(hireDate, cutoff).getYears());
    }

    public static int scale(int completedYears) {
        if (completedYears <= 5) {
            return 14;
        }
        if (completedYears <= 10) {
            return 21;
        }
        if (completedYears <= 20) {
            return 28;
        }
        return 35;
    }

    public static List<LocalDate> eachDate(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            dates.add(date);
        }
        return List.copyOf(dates);
    }

    /**
     * Recorre desde el ingreso para arrastrar lo no usado.
     * {@code fromPeriod} y {@code toPeriod} amplían el recorrido si hace falta mostrarlos.
     */
    public static List<PeriodBalance> balances(
            LocalDate hireDate,
            int fromPeriod,
            int toPeriod,
            Map<Integer, Integer> agreedByPeriod,
            Map<Integer, Integer> takenByPeriod,
            Map<Integer, Integer> workedByCalendarYear) {
        int hirePeriod = periodYear(hireDate);
        int start = Math.min(fromPeriod, hirePeriod);
        int end = Math.max(toPeriod, hirePeriod);
        List<PeriodBalance> rows = new ArrayList<>();
        int carry = 0;
        for (int year = start; year <= end; year++) {
            int legal = legalDays(hireDate, year, workedByCalendarYear.getOrDefault(year, 0));
            int agreed = agreedByPeriod.getOrDefault(year, 0);
            int taken = takenByPeriod.getOrDefault(year, 0);
            int usable = legal + agreed + carry;
            int remaining = usable - taken;
            rows.add(new PeriodBalance(year, legal, agreed, carry, taken, usable, remaining));
            carry = Math.max(0, remaining);
        }
        return List.copyOf(rows);
    }

    /**
     * @return mensaje de rechazo, o null si la carga entra en todos los períodos del rango
     */
    public static String rejection(
            String displayName,
            LocalDate hireDate,
            List<LocalDate> proposedDates,
            Map<Integer, Integer> agreedByPeriod,
            Map<Integer, Set<LocalDate>> takenDatesByPeriod,
            Map<Integer, Integer> workedByCalendarYear) {
        if (hireDate == null) {
            return "Falta la fecha de ingreso de " + displayName + ".";
        }
        if (proposedDates.isEmpty()) {
            return null;
        }
        int from = periodYear(proposedDates.get(0));
        int to = from;
        for (LocalDate date : proposedDates) {
            int year = periodYear(date);
            from = Math.min(from, year);
            to = Math.max(to, year);
        }
        int start = Math.min(from, periodYear(hireDate));
        int end = Math.max(to, periodYear(hireDate));
        List<String> problems = new ArrayList<>();
        int carry = 0;
        for (int year = start; year <= end; year++) {
            int legal = legalDays(hireDate, year, workedByCalendarYear.getOrDefault(year, 0));
            int agreed = agreedByPeriod.getOrDefault(year, 0);
            Set<LocalDate> taken = takenDatesByPeriod.getOrDefault(year, Set.of());
            int additional = 0;
            for (LocalDate date : proposedDates) {
                if (periodYear(date) == year && !taken.contains(date)) {
                    additional++;
                }
            }
            int usable = legal + agreed + carry;
            if (additional > 0 && taken.size() + additional > usable) {
                int remaining = Math.max(0, usable - taken.size());
                problems.add(remainingMessage(displayName, remaining, year));
            }
            carry = Math.max(0, usable - taken.size() - additional);
        }
        if (problems.isEmpty()) {
            return null;
        }
        return String.join(" ", problems);
    }

    private static String remainingMessage(String name, int remaining, int periodYear) {
        String days = remaining == 1
                ? "queda 1 día"
                : "quedan " + remaining + " días";
        return "A " + name + " le " + days + " en el período del " + periodLabel(periodYear) + ".";
    }
}
