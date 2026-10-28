package ar.com.anepanet.planificator.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cupo de vacaciones por período de goce. No lee turnos: los días gozados
 * entran como fechas del local vacaciones, así el franco no descuenta.
 */
public final class VacationQuota {

    /** Período que abre el 1 oct 2026. Lo anterior no entra en el saldo. */
    static final int FIRST_ENJOYMENT_YEAR = 2026;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private VacationQuota() {}

    public record EnjoymentSplit(int enjoyed, int scheduled) {}

    public record Snapshot(
            int thisPeriod,
            int carried,
            int extra,
            int enjoyed,
            int remaining) {

        /** Días habilitados: antigüedad de este período, más lo arrastrado, más el extra. */
        public int quota() {
            return thisPeriod + carried + extra;
        }
    }

    public static LocalDate businessToday() {
        return LocalDate.now(BUSINESS_ZONE);
    }

    /**
     * Gozados: fecha anterior a hoy. Por gozar: hoy y lo que sigue.
     * Solo cuenta días del período indicado.
     */
    public static EnjoymentSplit splitEnjoyment(Collection<LocalDate> dates, int periodYear, LocalDate today) {
        LocalDate start = periodStart(periodYear);
        LocalDate end = periodEnd(periodYear);
        int enjoyed = 0;
        int scheduled = 0;
        if (dates != null && today != null) {
            for (LocalDate date : dates) {
                if (date == null || date.isBefore(start) || date.isAfter(end)) {
                    continue;
                }
                if (date.isBefore(today)) {
                    enjoyed++;
                } else {
                    scheduled++;
                }
            }
        }
        return new EnjoymentSplit(enjoyed, scheduled);
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
        return seniorityDays(hire, periodYear, LocalDate.now(BUSINESS_ZONE));
    }

    /**
     * La escala usa la antigüedad al 31 de diciembre. La proporcional cuenta lunes a
     * viernes ya transcurridos, desde el ingreso hasta hoy, con ese mismo tope.
     */
    public static int seniorityDays(LocalDate hire, int periodYear, LocalDate today) {
        LocalDate cutoff = LocalDate.of(periodYear, 12, 31);
        if (hire == null || today == null || hire.isAfter(cutoff)) {
            return 0;
        }
        if (hire.plusMonths(6).isAfter(cutoff)) {
            LocalDate end = today.isBefore(cutoff) ? today : cutoff;
            if (end.isBefore(hire)) {
                return 0;
            }
            return weekdays(hire, end) / 20;
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

    /** Lunes a viernes entre las dos fechas, inclusive. Un feriado que cae en semana entra. */
    static int weekdays(LocalDate from, LocalDate to) {
        long span = ChronoUnit.DAYS.between(from, to) + 1;
        if (span <= 0) {
            return 0;
        }
        int count = (int) (span / 7) * 5;
        int extra = (int) (span % 7);
        DayOfWeek cursor = from.getDayOfWeek();
        for (int i = 0; i < extra; i++) {
            if (cursor.getValue() <= DayOfWeek.FRIDAY.getValue()) {
                count++;
            }
            cursor = cursor.plus(1);
        }
        return count;
    }

    public static Snapshot snapshot(
            LocalDate hire,
            int periodYear,
            Map<Integer, Integer> enjoyedByYear,
            Map<Integer, Integer> extraByYear) {
        return snapshot(hire, periodYear, enjoyedByYear, extraByYear, 0);
    }

    public static Snapshot snapshot(
            LocalDate hire,
            int periodYear,
            Map<Integer, Integer> enjoyedByYear,
            Map<Integer, Integer> extraByYear,
            int openingCarry) {
        int extra = daysOf(extraByYear, periodYear);
        int enjoyed = daysOf(enjoyedByYear, periodYear);
        if (hire == null || periodYear < hire.getYear()) {
            return new Snapshot(0, 0, extra, enjoyed, extra - enjoyed);
        }
        int seniority = seniorityDays(hire, periodYear);
        int thisPeriod = periodYear >= FIRST_ENJOYMENT_YEAR ? seniority : 0;
        int carried = carriedInto(hire, periodYear, enjoyedByYear, extraByYear, openingCarry);
        return new Snapshot(thisPeriod, carried, extra, enjoyed, thisPeriod + carried + extra - enjoyed);
    }

    public static boolean isOpeningPeriod(int periodYear) {
        return periodYear == FIRST_ENJOYMENT_YEAR;
    }

    /**
     * En el período 2026, Del período anterior es el valor cargado una sola vez.
     * Desde el siguiente, entra lo no gozado más la antigüedad que ya se habilitó.
     */
    private static int carriedInto(
            LocalDate hire,
            int periodYear,
            Map<Integer, Integer> enjoyedByYear,
            Map<Integer, Integer> extraByYear,
            int openingCarry) {
        int opening = Math.max(0, openingCarry);
        if (periodYear < FIRST_ENJOYMENT_YEAR) {
            return 0;
        }
        if (periodYear == FIRST_ENJOYMENT_YEAR) {
            return opening;
        }
        int carried = opening;
        for (int year = FIRST_ENJOYMENT_YEAR; year < periodYear; year++) {
            int quota = seniorityDays(hire, year) + carried + daysOf(extraByYear, year);
            carried = Math.max(0, quota - daysOf(enjoyedByYear, year));
        }
        return carried;
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
        return rejection(name, hire, enjoyedByYear, extraByYear, newDates, 0);
    }

    public static String rejection(
            String name,
            LocalDate hire,
            Map<Integer, Integer> enjoyedByYear,
            Map<Integer, Integer> extraByYear,
            List<LocalDate> newDates,
            int openingCarry) {
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
        List<Integer> years = new ArrayList<>(countByPeriod.keySet());
        Collections.sort(years);
        for (int year : years) {
            int requested = countByPeriod.get(year);
            Snapshot snap = snapshot(hire, year, enjoyed, extraByYear, openingCarry);
            if (snap.thisPeriod() == 0 && snap.carried() == 0 && snap.extra() == 0) {
                return name + " no tiene días de este período, del anterior ni extra";
            }
            if (snap.remaining() < requested) {
                return name + " no tiene saldo en el período " + periodLabel(year);
            }
            enjoyed.merge(year, requested, Integer::sum);
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
