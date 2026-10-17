package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.ShiftRepository;
import ar.com.anepanet.planificator.repository.VacationAgreementRepository;
import ar.com.anepanet.planificator.web.dto.VacationBalanceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class VacationQuotaService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ShiftRepository shifts;
    private final VacationAgreementRepository agreements;
    private final AuthRepository users;

    public VacationQuotaService(
            ShiftRepository shifts,
            VacationAgreementRepository agreements,
            AuthRepository users) {
        this.shifts = shifts;
        this.agreements = agreements;
        this.users = users;
    }

    public List<VacationBalanceResponse> preview(List<UUID> userIds, List<Integer> periodYears) {
        if (userIds == null || userIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elegí al menos una persona");
        }
        List<Integer> years = periodYears == null || periodYears.isEmpty()
                ? List.of(VacationQuota.periodYear(LocalDate.now(BUSINESS_ZONE)))
                : periodYears.stream().distinct().sorted().toList();
        List<VacationBalanceResponse> rows = new ArrayList<>();
        for (UUID userId : userIds.stream().distinct().toList()) {
            AppUser user = requireUser(userId);
            rows.addAll(balancesOf(user, years));
        }
        return rows;
    }

    @Transactional
    public void saveAgreedDays(UUID userId, int periodYear, int days) {
        requireUser(userId);
        agreements.upsert(userId, periodYear, days);
    }

    public void assertAll(
            List<AppUser> people,
            LocalDate from,
            LocalDate to,
            Map<UUID, Set<LocalDate>> ignoredVacationDates) {
        List<LocalDate> proposed = VacationQuota.eachDate(from, to);
        List<String> problems = new ArrayList<>();
        for (AppUser user : people) {
            String message = rejectionFor(
                    user,
                    proposed,
                    ignoredVacationDates.getOrDefault(user.id(), Set.of()));
            if (message != null) {
                problems.add(message);
            }
        }
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", problems));
        }
    }

    private List<VacationBalanceResponse> balancesOf(AppUser user, List<Integer> years) {
        String name = displayName(user);
        Map<Integer, Integer> agreed = agreements.findByUser(user.id());
        if (user.hireDate() == null) {
            return years.stream()
                    .map(year -> new VacationBalanceResponse(
                            user.id(),
                            name,
                            null,
                            true,
                            year,
                            VacationQuota.periodLabel(year),
                            0,
                            agreed.getOrDefault(year, 0),
                            0,
                            0,
                            0,
                            0))
                    .toList();
        }
        int from = years.get(0);
        int to = years.get(years.size() - 1);
        Usage usage = usage(user, from, to, Set.of());
        List<VacationQuota.PeriodBalance> walked = VacationQuota.balances(
                user.hireDate(), from, to, agreed, usage.takenCounts(), usage.workedByYear());
        List<VacationBalanceResponse> rows = new ArrayList<>();
        for (int year : years) {
            VacationQuota.PeriodBalance balance = walked.stream()
                    .filter(row -> row.periodYear() == year)
                    .findFirst()
                    .orElseThrow();
            rows.add(toResponse(user.id(), name, user.hireDate(), balance));
        }
        return rows;
    }

    private String rejectionFor(AppUser user, List<LocalDate> proposed, Set<LocalDate> ignored) {
        if (user.hireDate() == null) {
            return "Falta la fecha de ingreso de " + displayName(user) + ".";
        }
        int from = VacationQuota.periodYear(proposed.get(0));
        int to = from;
        for (LocalDate date : proposed) {
            int year = VacationQuota.periodYear(date);
            from = Math.min(from, year);
            to = Math.max(to, year);
        }
        Usage usage = usage(user, from, to, ignored);
        return VacationQuota.rejection(
                displayName(user),
                user.hireDate(),
                proposed,
                agreements.findByUser(user.id()),
                usage.takenDates(),
                usage.workedByYear());
    }

    private Usage usage(AppUser user, int fromPeriod, int toPeriod, Set<LocalDate> ignored) {
        int hirePeriod = VacationQuota.periodYear(user.hireDate());
        int start = Math.min(fromPeriod, hirePeriod);
        int end = Math.max(toPeriod, hirePeriod);
        List<LocalDate> vacationDates = shifts.vacationDates(
                user.id(), VacationQuota.periodStart(start), VacationQuota.periodEnd(end));
        Map<Integer, Set<LocalDate>> takenDates = new HashMap<>();
        for (LocalDate date : vacationDates) {
            if (ignored.contains(date)) {
                continue;
            }
            takenDates.computeIfAbsent(VacationQuota.periodYear(date), ignoredYear -> new HashSet<>())
                    .add(date);
        }
        Map<Integer, Integer> takenCounts = new HashMap<>();
        takenDates.forEach((year, dates) -> takenCounts.put(year, dates.size()));
        List<LocalDate> workedDates = shifts.workedDates(
                user.id(), LocalDate.of(start, 1, 1), LocalDate.of(end, 12, 31));
        Map<Integer, Integer> worked = new HashMap<>();
        for (LocalDate date : workedDates) {
            worked.merge(date.getYear(), 1, Integer::sum);
        }
        return new Usage(takenDates, takenCounts, worked);
    }

    private AppUser requireUser(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada"));
    }

    private static VacationBalanceResponse toResponse(
            UUID userId, String name, LocalDate hireDate, VacationQuota.PeriodBalance balance) {
        return new VacationBalanceResponse(
                userId,
                name,
                hireDate,
                false,
                balance.periodYear(),
                VacationQuota.periodLabel(balance.periodYear()),
                balance.legalDays(),
                balance.agreedDays(),
                balance.accumulatedDays(),
                balance.takenDays(),
                balance.usableDays(),
                balance.remainingDays());
    }

    private static String displayName(AppUser user) {
        if (user.displayName() == null || user.displayName().isBlank()) {
            return user.username();
        }
        return user.displayName();
    }

    private record Usage(
            Map<Integer, Set<LocalDate>> takenDates,
            Map<Integer, Integer> takenCounts,
            Map<Integer, Integer> workedByYear) {}
}
