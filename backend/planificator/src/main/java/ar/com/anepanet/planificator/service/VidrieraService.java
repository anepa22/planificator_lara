package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.Vidriera;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.VidrieraRepository;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.security.SecurityUtils;
import ar.com.anepanet.planificator.web.dto.UpsertVidrieraRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class VidrieraService {

    private final VidrieraRepository vidrieras;
    private final LocationRepository locations;
    private final AuditService audit;

    public VidrieraService(
            VidrieraRepository vidrieras,
            LocationRepository locations,
            AuditService audit) {
        this.vidrieras = vidrieras;
        this.locations = locations;
        this.audit = audit;
    }

    public List<Vidriera> byRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from y to son obligatorios");
        }
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from no puede ser posterior a to");
        }
        return vidrieras.findByDateRange(from, to);
    }

    public Vidriera upsert(UpsertVidrieraRequest req) {
        SecurityUtils.requireAuthority(Permissions.SHIFTS_WRITE);
        Location location = ensureVidrieraLocation(req.locationId());
        boolean created = vidrieras.insert(req.locationId(), req.workDate());
        Vidriera row = vidrieras.findOne(req.locationId(), req.workDate())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la vidriera"));
        if (created) {
            audit.record(
                    AuditService.ACTION_CREATE,
                    AuditService.TYPE_VIDRIERA,
                    entityId(row),
                    location.name() + " · " + row.workDate()
            );
        }
        return row;
    }

    public void delete(String locationId, LocalDate workDate) {
        SecurityUtils.requireAuthority(Permissions.SHIFTS_WRITE);
        if (locationId == null || locationId.isBlank() || workDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locationId y workDate son obligatorios");
        }
        Location location = locations.findById(locationId).orElse(null);
        boolean deleted = vidrieras.delete(locationId, workDate);
        if (deleted) {
            String name = location != null ? location.name() : locationId;
            audit.record(
                    AuditService.ACTION_DELETE,
                    AuditService.TYPE_VIDRIERA,
                    locationId + "|" + workDate,
                    name + " · " + workDate
            );
        }
    }

    private Location ensureVidrieraLocation(String id) {
        Location location = locations.findById(id)
                .filter(Location::active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local inválido o inactivo"));
        if (Permissions.isAbsenceLocation(location.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vacaciones y franco no admiten vidriera");
        }
        if (!location.supportsVidriera()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este local no admite vidriera");
        }
        return location;
    }

    private static String entityId(Vidriera row) {
        return row.locationId() + "|" + row.workDate();
    }
}
