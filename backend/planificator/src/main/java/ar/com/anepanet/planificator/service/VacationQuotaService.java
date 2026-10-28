package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.VacationPersonReport;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.ShiftRepository;
import ar.com.anepanet.planificator.repository.VacationAdjustmentRepository;
import ar.com.anepanet.planificator.repository.VacationOpeningCarryRepository;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.security.SecurityUtils;
import ar.com.anepanet.planificator.web.dto.VacationQuotaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VacationQuotaService {

    private final AuthRepository users;
    private final ShiftRepository shifts;
    private final VacationAdjustmentRepository adjustments;
    private final VacationOpeningCarryRepository openingCarry;

    public VacationQuotaService(
            AuthRepository users,
            ShiftRepository shifts,
            VacationAdjustmentRepository adjustments,
            VacationOpeningCarryRepository openingCarry) {
        this.users = users;
        this.shifts = shifts;
        this.adjustments = adjustments;
        this.openingCarry = openingCarry;
    }

    public VacationQuotaResponse quota(UUID userId, int periodYear) {
        SecurityUtils.requireAuthority(Permissions.VACATIONS_WRITE);
        requirePeriodYear(periodYear);
        return response(requireUser(userId), periodYear);
    }

    public VacationQuotaResponse saveAdjustment(UUID userId, int periodYear, int days, Integer carried) {
        SecurityUtils.requireAuthority(Permissions.VACATIONS_WRITE);
        requirePeriodYear(periodYear);
        try {
            VacationQuota.requireAdjustment(days);
            if (carried != null) {
                VacationQuota.requireAdjustment(carried);
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        if (carried != null && !VacationQuota.isOpeningPeriod(periodYear)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Del período anterior solo se carga en el período que abre el 1 de octubre de 2026");
        }
        requireUser(userId);
        adjustments.save(userId, periodYear, days);
        if (carried != null) {
            openingCarry.save(userId, carried);
        }
        return response(requireUser(userId), periodYear);
    }

    public void assertCanLoad(List<UUID> userIds, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisá las fechas");
        }
        List<LocalDate> proposed = VacationQuota.eachDate(from, to);
        for (UUID userId : userIds) {
            AppUser user = requireUser(userId);
            List<LocalDate> already = shifts.findVacationDates(userId, from, to);
            List<LocalDate> fresh = VacationQuota.newDates(proposed, already);
            int throughYear = VacationQuota.periodYear(to);
            String message = VacationQuota.rejection(
                    displayName(user),
                    user.hireDate(),
                    enjoyedByYear(user, throughYear),
                    adjustments.findByUser(userId),
                    fresh,
                    openingCarry.findDays(userId));
            if (message != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
        }
    }

    public VacationPersonReport withQuota(VacationPersonReport person, int periodYear) {
        AppUser user = users.findById(person.userId()).orElse(null);
        if (user == null || user.hireDate() == null) {
            return person;
        }
        VacationQuota.Snapshot snap = VacationQuota.snapshot(
                user.hireDate(),
                periodYear,
                enjoyedByYear(user, periodYear),
                adjustments.findByUser(user.id()),
                openingCarry.findDays(user.id()));
        return new VacationPersonReport(
                person.userId(),
                person.userName(),
                person.days(),
                person.ranges(),
                snap.thisPeriod(),
                snap.carried(),
                snap.extra(),
                snap.enjoyed(),
                snap.remaining());
    }

    private VacationQuotaResponse response(AppUser user, int periodYear) {
        if (user.hireDate() == null) {
            return new VacationQuotaResponse(
                    user.id(),
                    displayName(user),
                    null,
                    periodYear,
                    VacationQuota.periodLabel(periodYear),
                    null, null, null, null, null);
        }
        VacationQuota.Snapshot snap = VacationQuota.snapshot(
                user.hireDate(),
                periodYear,
                enjoyedByYear(user, periodYear),
                adjustments.findByUser(user.id()),
                openingCarry.findDays(user.id()));
        return new VacationQuotaResponse(
                user.id(),
                displayName(user),
                user.hireDate(),
                periodYear,
                VacationQuota.periodLabel(periodYear),
                snap.thisPeriod(),
                snap.carried(),
                snap.extra(),
                snap.enjoyed(),
                snap.remaining());
    }

    private Map<Integer, Integer> enjoyedByYear(AppUser user, int throughYear) {
        Map<Integer, Integer> enjoyed = new HashMap<>();
        if (user.hireDate() == null) {
            return enjoyed;
        }
        int fromYear = user.hireDate().getYear();
        int toYear = Math.max(fromYear, throughYear);
        List<LocalDate> dates = shifts.findVacationDates(
                user.id(),
                VacationQuota.periodStart(fromYear),
                VacationQuota.periodEnd(toYear));
        for (LocalDate date : dates) {
            enjoyed.merge(VacationQuota.periodYear(date), 1, Integer::sum);
        }
        return enjoyed;
    }

    private AppUser requireUser(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Persona inválida"));
    }

    private static void requirePeriodYear(int periodYear) {
        if (periodYear < 1990 || periodYear > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período inválido");
        }
    }

    private static String displayName(AppUser user) {
        if (user.displayName() == null || user.displayName().isBlank()) {
            return user.username();
        }
        return user.displayName();
    }
}
