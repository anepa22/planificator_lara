package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.HoursByPersonWeek;
import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.Person;
import ar.com.anepanet.planificator.domain.Shift;
import ar.com.anepanet.planificator.domain.ShiftWeekView;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.PersonRepository;
import ar.com.anepanet.planificator.repository.ShiftRepository;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.security.SecurityUtils;
import ar.com.anepanet.planificator.web.dto.CreateShiftRequest;
import ar.com.anepanet.planificator.web.dto.UpdateShiftRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Service
public class ShiftService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ShiftRepository shifts;
    private final PersonRepository people;
    private final LocationRepository locations;
    private final AuditService audit;

    public ShiftService(
            ShiftRepository shifts,
            PersonRepository people,
            LocationRepository locations,
            AuditService audit) {
        this.shifts = shifts;
        this.people = people;
        this.locations = locations;
        this.audit = audit;
    }

    public List<ShiftWeekView> byWeek(LocalDate weekStart) {
        return shifts.findByWeekStart(normalizeWeekStart(weekStart));
    }

    public List<HoursByPersonWeek> hoursByWeek(LocalDate weekStart) {
        return shifts.hoursByWeek(normalizeWeekStart(weekStart));
    }

    public Shift create(CreateShiftRequest req) {
        SecurityUtils.requireLocationWrite(req.locationId());
        validateRange(req.startTime(), req.endTime());
        Person person = ensurePerson(req.personId());
        Location location = ensureLocation(req.locationId());
        try {
            Shift created = shifts.insert(
                    req.personId(),
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
                    person.name()
                            + " · " + location.name()
                            + " · " + created.workDate()
                            + " " + fmtTime(created.startTime()) + "–" + fmtTime(created.endTime())
            );
            return created;
        } catch (DataIntegrityViolationException ex) {
            throw conflict(ex);
        }
    }

    public Shift update(UUID id, UpdateShiftRequest req) {
        Shift existing = shifts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        SecurityUtils.requireLocationWrite(existing.locationId());
        SecurityUtils.requireLocationWrite(req.locationId());
        validateRange(req.startTime(), req.endTime());
        Location location = ensureLocation(req.locationId());
        Person person = people.findById(existing.personId()).orElse(null);
        try {
            Shift updated = shifts.update(
                            id, req.locationId(), req.workDate(), req.startTime(), req.endTime(), req.notes())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
            audit.record(
                    AuditService.ACTION_UPDATE,
                    auditEntityType(updated.locationId()),
                    updated.id().toString(),
                    (person != null ? person.name() : existing.personId())
                            + " · " + location.name()
                            + " · " + updated.workDate()
                            + " " + fmtTime(updated.startTime()) + "–" + fmtTime(updated.endTime())
            );
            return updated;
        } catch (DataIntegrityViolationException ex) {
            throw conflict(ex);
        }
    }

    public void delete(UUID id) {
        Shift existing = shifts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
        SecurityUtils.requireLocationWrite(existing.locationId());
        Person person = people.findById(existing.personId()).orElse(null);
        Location location = locations.findById(existing.locationId()).orElse(null);
        if (!shifts.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado");
        }
        audit.record(
                AuditService.ACTION_DELETE,
                auditEntityType(existing.locationId()),
                id.toString(),
                (person != null ? person.name() : existing.personId())
                        + " · " + (location != null ? location.name() : existing.locationId())
                        + " · " + existing.workDate()
                        + " " + fmtTime(existing.startTime()) + "–" + fmtTime(existing.endTime())
        );
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

    private Person ensurePerson(UUID id) {
        return people.findById(id)
                .filter(Person::active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Persona inválida o inactiva"));
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
        if (msg != null && msg.contains("shifts_no_person_time_overlap")) {
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    "La persona ya tiene un turno que se solapa en ese horario");
        }
        return new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo guardar el turno: conflicto de datos");
    }
}
