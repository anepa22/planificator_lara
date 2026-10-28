package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.HoursReportLocationOption;
import ar.com.anepanet.planificator.domain.HoursReportPerson;
import ar.com.anepanet.planificator.domain.HoursReportPersonOption;
import ar.com.anepanet.planificator.domain.HoursShiftRow;
import ar.com.anepanet.planificator.domain.StaffMember;
import ar.com.anepanet.planificator.security.Permissions;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class HoursReportMath {

    static final BigDecimal DEFAULT_LUNCH = new BigDecimal("1");
    static final BigDecimal MAX_LUNCH = new BigDecimal("4");
    static final long MAX_RANGE_DAYS = 800;

    private HoursReportMath() {}

    static void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indicá desde y hasta");
        }
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hasta no puede ser anterior a desde");
        }
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (periodDays > MAX_RANGE_DAYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El período no puede superar " + MAX_RANGE_DAYS + " días");
        }
    }

    static BigDecimal requireLunch(BigDecimal lunch) {
        if (lunch == null) {
            return DEFAULT_LUNCH.setScale(2, RoundingMode.HALF_UP);
        }
        if (lunch.compareTo(BigDecimal.ZERO) < 0 || lunch.compareTo(MAX_LUNCH) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El almuerzo tiene que estar entre 0 y 4 horas");
        }
        return lunch.setScale(2, RoundingMode.HALF_UP);
    }

    static List<HoursReportPerson> people(
            List<HoursShiftRow> shifts,
            UUID personId,
            String selectedPersonName,
            String locationId,
            BigDecimal lunch) {
        Map<UUID, Accumulator> byUser = new LinkedHashMap<>();
        for (HoursShiftRow shift : shifts) {
            if (Permissions.isAbsenceLocation(shift.locationId())) {
                continue;
            }
            if (locationId != null && !locationId.equals(shift.locationId())) {
                continue;
            }
            if (personId != null && !personId.equals(shift.userId())) {
                continue;
            }
            Accumulator acc = byUser.computeIfAbsent(shift.userId(), ignored -> new Accumulator(shift.userName()));
            acc.name = shift.userName();
            acc.gross = acc.gross.add(duration(shift.startTime(), shift.endTime()));
            acc.days.add(shift.workDate());
        }

        List<HoursReportPerson> people = new ArrayList<>();
        byUser.forEach((userId, acc) -> {
            BigDecimal net = net(acc.gross, lunch, acc.days.size());
            if (personId == null && net.signum() == 0) {
                return;
            }
            people.add(new HoursReportPerson(userId, acc.name, net));
        });
        if (personId != null && byUser.get(personId) == null) {
            people.add(new HoursReportPerson(personId, selectedPersonName, BigDecimal.ZERO.setScale(2)));
        }
        people.sort(Comparator
                .comparing(HoursReportPerson::userName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(person -> person.userId().toString()));
        return List.copyOf(people);
    }

    static BigDecimal total(List<HoursReportPerson> people) {
        BigDecimal sum = BigDecimal.ZERO;
        for (HoursReportPerson person : people) {
            sum = sum.add(person.hours());
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    static List<HoursReportLocationOption> locationOptions(
            List<LocationChoice> locations, Set<String> usedInRange) {
        List<HoursReportLocationOption> options = new ArrayList<>();
        for (LocationChoice location : locations) {
            if (Permissions.isAbsenceLocation(location.id())) {
                continue;
            }
            if (location.active() || usedInRange.contains(location.id())) {
                options.add(new HoursReportLocationOption(location.id(), location.name()));
            }
        }
        return List.copyOf(options);
    }

    static Set<String> salesLocationIds(List<HoursShiftRow> shifts) {
        Set<String> ids = new LinkedHashSet<>();
        for (HoursShiftRow shift : shifts) {
            if (!Permissions.isAbsenceLocation(shift.locationId())) {
                ids.add(shift.locationId());
            }
        }
        return ids;
    }

    static List<HoursReportPersonOption> personOptions(
            List<StaffMember> staff, List<HoursShiftRow> shifts) {
        Map<UUID, String> names = new LinkedHashMap<>();
        for (StaffMember member : staff) {
            names.put(member.id(), member.name());
        }
        for (HoursShiftRow shift : shifts) {
            if (Permissions.isAbsenceLocation(shift.locationId())) {
                continue;
            }
            names.putIfAbsent(shift.userId(), shift.userName());
        }
        List<HoursReportPersonOption> options = new ArrayList<>();
        names.forEach((id, name) -> options.add(new HoursReportPersonOption(id, name)));
        options.sort(Comparator
                .comparing(HoursReportPersonOption::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(option -> option.id().toString()));
        return List.copyOf(options);
    }

    private static BigDecimal duration(LocalTime start, LocalTime end) {
        int minutes = end.toSecondOfDay() / 60 - start.toSecondOfDay() / 60;
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal net(BigDecimal gross, BigDecimal lunch, int days) {
        BigDecimal value = gross.subtract(lunch.multiply(BigDecimal.valueOf(days)));
        if (value.signum() < 0) {
            value = BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    record LocationChoice(String id, String name, boolean active) {}

    private static final class Accumulator {
        private String name;
        private BigDecimal gross = BigDecimal.ZERO;
        private final Set<LocalDate> days = new LinkedHashSet<>();

        private Accumulator(String name) {
            this.name = name;
        }
    }
}
