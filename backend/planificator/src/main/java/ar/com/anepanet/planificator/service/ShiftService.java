package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.HoursByUserWeek;
import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.Shift;
import ar.com.anepanet.planificator.domain.ShiftWeekView;
import ar.com.anepanet.planificator.domain.VacationDay;
import ar.com.anepanet.planificator.domain.VacationPersonReport;
import ar.com.anepanet.planificator.domain.VacationRange;
import ar.com.anepanet.planificator.domain.VacationReport;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.ShiftRepository;
import ar.com.anepanet.planificator.realtime.PlanificatorChangedEvent;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.security.SecurityUtils;
import ar.com.anepanet.planificator.web.dto.CreateShiftRequest;
import ar.com.anepanet.planificator.web.dto.UpdateShiftRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShiftService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final long MAX_REPORT_RANGE_DAYS = 800;

    private final ShiftRepository shifts;
    private final AuthRepository users;
    private final LocationRepository locations;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public ShiftService(
            ShiftRepository shifts,
            AuthRepository users,
            LocationRepository locations,
            AuditService audit,
            ApplicationEventPublisher events) {
        this.shifts = shifts;
        this.users = users;
        this.locations = locations;
        this.audit = audit;
        this.events = events;
    }

    public List<ShiftWeekView> byWeek(LocalDate weekStart) {
        return shifts.findByWeekStart(normalizeWeekStart(weekStart));
    }

    public List<HoursByUserWeek> hoursByWeek(LocalDate weekStart) {
        return shifts.hoursByWeek(normalizeWeekStart(weekStart));
    }

    public VacationReport vacationReport(LocalDate from, LocalDate to) {
        SecurityUtils.requireAuthority(Permissions.VACATIONS_REPORT);
        return loadVacationReport(from, to);
    }

    public byte[] vacationReportPdf(LocalDate from, LocalDate to) {
        SecurityUtils.requireAuthority(Permissions.VACATIONS_PRINT);
        return VacationReportPdf.render(loadVacationReport(from, to));
    }

    private VacationReport loadVacationReport(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indicá desde y hasta");
        }
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hasta no puede ser anterior a desde");
        }
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (periodDays > MAX_REPORT_RANGE_DAYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El período no puede superar " + MAX_REPORT_RANGE_DAYS + " días");
        }
        List<VacationPersonReport> people = groupVacationDays(shifts.findVacationDays(from, to));
        int totalDays = people.stream().mapToInt(VacationPersonReport::days).sum();
        return new VacationReport(from, to, totalDays, people);
    }

    static List<VacationPersonReport> groupVacationDays(List<VacationDay> days) {
        Map<UUID, List<VacationDay>> byUser = new LinkedHashMap<>();
        for (VacationDay day : days) {
            byUser.computeIfAbsent(day.userId(), ignored -> new ArrayList<>()).add(day);
        }

        List<VacationPersonReport> people = new ArrayList<>();
        byUser.forEach((userId, userDays) -> {
            userDays.sort(Comparator.comparing(VacationDay::workDate));
            people.add(person(
                    userId,
                    userDays.get(0).userName(),
                    contiguousRanges(userDays)));
        });
        return List.copyOf(people);
    }

    private static List<VacationRange> contiguousRanges(List<VacationDay> days) {
        List<VacationRange> ranges = new ArrayList<>();
        LocalDate runFrom = null;
        LocalDate runTo = null;
        for (VacationDay day : days) {
            if (runFrom == null) {
                runFrom = day.workDate();
                runTo = day.workDate();
            } else if (day.workDate().equals(runTo.plusDays(1))) {
                runTo = day.workDate();
            } else {
                ranges.add(range(runFrom, runTo));
                runFrom = day.workDate();
                runTo = day.workDate();
            }
        }
        if (runFrom != null) {
            ranges.add(range(runFrom, runTo));
        }
        return List.copyOf(ranges);
    }

    private static VacationRange range(LocalDate from, LocalDate to) {
        int days = (int) ChronoUnit.DAYS.between(from, to) + 1;
        return new VacationRange(from, to, days);
    }

    private static VacationPersonReport person(UUID id, String name, List<VacationRange> ranges) {
        int days = ranges.stream().mapToInt(VacationRange::days).sum();
        return new VacationPersonReport(id, name, days, List.copyOf(ranges));
    }

    @Transactional
    public Shift create(CreateShiftRequest req) {
        SecurityUtils.requireLocationWrite(req.locationId());
        validateRange(req.startTime(), req.endTime());
        AppUser user = ensureUser(req.userId());
        Location location = ensureLocation(req.locationId());
        try {
            Shift created = shifts.insert(
                    req.userId(),
                    req.locationId(),
                    req.workDate(),
                    req.startTime(),
                    req.endTime(),
                    req.notes()
            );
            audit.record(
                    AuditService.ACTION_CREATE,
                    auditEntityType(created.locationId()),
                    created.id().toString(),
                    displayName(user)
                            + " · " + location.name()
                            + " · " + created.workDate()
                            + " " + fmtTime(created.startTime()) + "–" + fmtTime(created.endTime())
            );
            publishRealtimeScheduleChange();
            return created;
        } catch (DataIntegrityViolationException ex) {
            throw conflict(ex);
        }
    }

    @Transactional
    public Shift update(UUID id, UpdateShiftRequest req) {
        Shift existing = shifts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        SecurityUtils.requireLocationWrite(existing.locationId());
        SecurityUtils.requireLocationWrite(req.locationId());
        validateRange(req.startTime(), req.endTime());
        Location location = ensureLocation(req.locationId());
        String who = nameOf(existing.userId());
        try {
            Shift updated = shifts.update(
                            id, req.locationId(), req.workDate(), req.startTime(), req.endTime(), req.notes())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
            audit.record(
                    AuditService.ACTION_UPDATE,
                    auditEntityType(updated.locationId()),
                    updated.id().toString(),
                    who
                            + " · " + location.name()
                            + " · " + updated.workDate()
                            + " " + fmtTime(updated.startTime()) + "–" + fmtTime(updated.endTime())
            );
            publishRealtimeScheduleChange();
            return updated;
        } catch (DataIntegrityViolationException ex) {
            throw conflict(ex);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Shift existing = shifts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        SecurityUtils.requireLocationWrite(existing.locationId());
        String who = nameOf(existing.userId());
        Location location = locations.findById(existing.locationId()).orElse(null);
        if (!shifts.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado");
        }
        audit.record(
                AuditService.ACTION_DELETE,
                auditEntityType(existing.locationId()),
                id.toString(),
                who
                        + " · " + (location != null ? location.name() : existing.locationId())
                        + " · " + existing.workDate()
                        + " " + fmtTime(existing.startTime()) + "–" + fmtTime(existing.endTime())
        );
        publishRealtimeScheduleChange();
    }

    private void publishRealtimeScheduleChange() {
        events.publishEvent(new PlanificatorChangedEvent(PlanificatorChangedEvent.SCHEDULE));
    }

    private static String auditEntityType(String locationId) {
        if (Permissions.VACATION_LOCATION_ID.equals(locationId)) {
            return AuditService.TYPE_VACATION;
        }
        if (Permissions.FRANCO_LOCATION_ID.equals(locationId)) {
            return AuditService.TYPE_FRANCO;
        }
        return AuditService.TYPE_SHIFT;
    }

    private AppUser ensureUser(UUID id) {
        return users.findById(id)
                .filter(AppUser::active)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Persona inválida o inactiva"));
    }

    private String nameOf(UUID userId) {
        return users.findById(userId)
                .map(ShiftService::displayName)
                .orElseGet(() -> String.valueOf(userId));
    }

    private static String displayName(AppUser user) {
        if (user.displayName() == null || user.displayName().isBlank()) {
            return user.username();
        }
        return user.displayName();
    }

    private Location ensureLocation(String id) {
        return locations.findById(id)
                .filter(Location::active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local inválido o inactivo"));
    }

    private void validateRange(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior al inicio");
        }
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static String fmtTime(LocalTime t) {
        return t == null ? "" : t.format(TIME_FMT);
    }

    private ResponseStatusException conflict(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains("shifts_no_user_time_overlap")) {
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    "La persona ya tiene un turno que se solapa en ese horario");
        }
        return new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo guardar el turno: conflicto de datos");
    }
}
