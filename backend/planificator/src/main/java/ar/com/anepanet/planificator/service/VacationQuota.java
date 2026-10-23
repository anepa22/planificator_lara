package ar.com.anepanet.planificator.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cupo de vacaciones por período de goce. No lee turnos: los días gozados
 * entran como fechas del local vacaciones, así el franco no descuenta.
 */
public final class VacationQuota {

    private VacationQuota() {}

    public record Snapshot(
            int thisPeriod,
            int carried,
            int extra,
            int enjoyed,
            int remaining) {

        public int quota() {
            return thisPeriod + carried + extra;
        }
    }

    public static int periodYear(LocalDate date) {
        return date.getMonthValue() >= 10 ? date.getYear() : date.getYear() - 1;
    }

    public static LocalDate periodStart(int year) {
        return LocalDate.of(year, 10, 1);
    }

    public static LocalDate periodEnd(int year) {
        return LocalDate.of(year + 1, 9, 30);
    }

    public static boolean isEnjoymentPeriod(LocalDate from, LocalDate to) {
        return from != null
                && to != null
                && from.getMonthValue() == 10
                && from.getDayOfMonth() == 1
                && to.equals(from.plusYears(1).minusDays(1));
    }

    public static String periodLabel(int year) {
        return "1 oct " + year + " – 30 sep " + (year + 1);
    }

    public static List<LocalDate> eachDate(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            dates.add(date);
        }
        return List.copyOf(dates);
    }

    /** Fechas del pedido que todavía no son vacaciones de esa persona. */
    public static List<LocalDate> newDates(List<LocalDate> proposed, Collection<LocalDate> alreadyVacation) {
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date : proposed) {
            if (!alreadyVacation.contains(date)) {
                dates.add(date);
            }
        }
        return List.copyOf(dates);
    }

    public static int seniorityDays(LocalDate hire, int periodYear) {
        LocalDate cutoff = LocalDate.of(periodYear, 12, 31);
        if (hire == null || hire.isAfter(cutoff)) {
            return 0;
        }
        if (hire.plusMonths(6).isAfter(cutoff)) {
            long calendarDays = ChronoUnit.DAYS.between(hire, cutoff) + 1;
            return (int) (calendarDays / 20);
        }
        if (hire.plusYears(20).isBefore(cutoff)) {
            return 35;
        }
        if (hire.plusYears(10).isBefore(cutoff)) {
            return 28;
        }
        if (hire.plusYears(5).isBefore(cutoff)) {
            return 21;
        }
        return 14;
    }

    public static Snapshot snapshot(
            LocalDate hire,
            int periodYear,
            Map<Integer, Integer> enjoyedByYear,
            Map<Integer, Integer> extraByYear) {
        int extra = daysOf(extraByYear, periodYear);
        int enjoyed = daysOf(enjoyedByYear, periodYear);
        if (hire == null || periodYear < hire.getYear()) {
            return new Snapshot(0, 0, extra, enjoyed, extra - enjoyed);
        }
        int carried = 0;
        for (int year = hire.getYear(); year < periodYear; year++) {
            int quota = seniorityDays(hire, year) + carried + daysOf(extraByYear, year);
            carried = Math.max(0, quota - daysOf(enjoyedByYear, year));
        }
        int thisPeriod = seniorityDays(hire, periodYear);
        int remaining = thisPeriod + carried + extra - enjoyed;
        return new Snapshot(thisPeriod, carried, extra, enjoyed, remaining);
    }

    /**
     * @return mensaje de rechazo, o null si las fechas nuevas entran en el saldo
     */
    public static String rejection(
            String name,
            LocalDate hire,
            Map<Integer, Integer> enjoyedByYear,
            Map<Integer, Integer> extraByYear,
            List<LocalDate> newDates) {
        if (newDates.isEmpty()) {
            return null;
        }
        if (hire == null) {
            return name + " no tiene fecha de ingreso";
        }
        Map<Integer, Integer> countByPeriod = new LinkedHashMap<>();
        for (LocalDate date : newDates) {
            countByPeriod.merge(periodYear(date), 1, Integer::sum);
        }
        Map<Integer, Integer> enjoyed = new LinkedHashMap<>();
        if (enjoyedByYear != null) {
            enjoyed.putAll(enjoyedByYear);
        }
        for (Map.Entry<Integer, Integer> entry : countByPeriod.entrySet()) {
            Snapshot snap = snapshot(hire, entry.getKey(), enjoyed, extraByYear);
            if (snap.thisPeriod() == 0 && snap.carried() == 0 && snap.extra() == 0) {
                return name + " no tiene días de este período, del anterior ni extra";
            }
            if (snap.remaining() < entry.getValue()) {
                return name + " no tiene saldo en el período " + periodLabel(entry.getKey());
            }
            enjoyed.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return null;
    }

    public static void requireAdjustment(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("El ajuste no puede ser negativo");
        }
    }

    private static int daysOf(Map<Integer, Integer> days, int year) {
        if (days == null) {
            return 0;
        }
        return days.getOrDefault(year, 0);
    }
}
