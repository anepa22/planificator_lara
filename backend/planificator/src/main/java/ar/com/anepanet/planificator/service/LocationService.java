package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.security.SecurityUtils;
import ar.com.anepanet.planificator.web.dto.SaveLocationRequest;
import ar.com.anepanet.planificator.web.dto.UpdateLocationRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class LocationService {

    static final int MAX_ACTIVE_SALES = 8;

    private final LocationRepository locations;
    private final AuditService audit;

    public LocationService(LocationRepository locations, AuditService audit) {
        this.locations = locations;
        this.audit = audit;
    }

    public List<Location> list() {
        return locations.findAllActive();
    }

    public List<Location> listManageable() {
        SecurityUtils.requireAuthority(Permissions.LOCATIONS_MANAGE);
        return locations.findAll().stream()
                .filter(location -> !Permissions.isAbsenceLocation(location.id()))
                .toList();
    }

    @Transactional
    public Location create(SaveLocationRequest req) {
        SecurityUtils.requireAuthority(Permissions.LOCATIONS_MANAGE);
        String name = normalizeName(req.name());
        requireName(name);
        String color = normalizeColor(req.color());
        boolean vidriera = Boolean.TRUE.equals(req.supportsVidriera());
        String id = slug(name);
        if (locations.idExists(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un local con ese identificador");
        }
        if (locations.nameTaken(nameKey(name), "")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un local con ese nombre");
        }
        requireRoom();
        try {
            locations.insert(id, name, color, colorSoft(color), locations.nextSortOrder(), vidriera);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un local con ese nombre");
        }
        Location created = require(id);
        audit.record(AuditService.ACTION_CREATE, AuditService.TYPE_LOCATION, created.id(), "Alta de local " + created.name());
        return created;
    }

    @Transactional
    public Location update(String id, UpdateLocationRequest req) {
        SecurityUtils.requireAuthority(Permissions.LOCATIONS_MANAGE);
        Location current = requireSales(id);
        String name = normalizeName(req.name());
        requireName(name);
        if (locations.nameTaken(nameKey(name), current.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un local con ese nombre");
        }
        String color = normalizeColor(req.color());
        short order = sortOrder(req.sortOrder(), current.sortOrder());
        boolean vidriera = req.supportsVidriera() == null
                ? current.supportsVidriera()
                : req.supportsVidriera();
        save(current.id(), name, color, order, current.active(), vidriera);
        Location updated = require(current.id());
        audit.record(AuditService.ACTION_UPDATE, AuditService.TYPE_LOCATION, updated.id(), "Edición de local " + updated.name());
        return updated;
    }

    @Transactional
    public Location deactivate(String id) {
        SecurityUtils.requireAuthority(Permissions.LOCATIONS_MANAGE);
        Location current = requireSales(id);
        if (current.active()) {
            save(current.id(), current.name(), current.color(), current.sortOrder(), false, current.supportsVidriera());
            audit.record(AuditService.ACTION_DELETE, AuditService.TYPE_LOCATION, current.id(), "Baja de local " + current.name());
        }
        return require(current.id());
    }

    @Transactional
    public Location activate(String id) {
        SecurityUtils.requireAuthority(Permissions.LOCATIONS_MANAGE);
        Location current = requireSales(id);
        if (!current.active()) {
            requireRoom();
            save(current.id(), current.name(), current.color(), current.sortOrder(), true, current.supportsVidriera());
            audit.record(AuditService.ACTION_UPDATE, AuditService.TYPE_LOCATION, current.id(), "Alta de local " + current.name());
        }
        return require(current.id());
    }

    private void save(
            String id,
            String name,
            String color,
            short sortOrder,
            boolean active,
            boolean supportsVidriera) {
        try {
            int rows = locations.update(id, name, color, colorSoft(color), sortOrder, active, supportsVidriera);
            if (rows == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Local no encontrado");
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un local con ese nombre");
        }
    }

    private Location requireSales(String id) {
        if (id == null || Permissions.isAbsenceLocation(id.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Franco y vacaciones no se administran desde el catálogo");
        }
        return require(id.trim());
    }

    private Location require(String id) {
        return locations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Local no encontrado"));
    }

    private void requireRoom() {
        if (locations.countActiveSales() >= MAX_ACTIVE_SALES) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya hay " + MAX_ACTIVE_SALES + " locales activos. Dad uno de baja para agregar otro.");
        }
    }

    private static void requireName(String name) {
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        String folded = fold(name);
        if ("franco".equals(folded) || "vacaciones".equals(folded)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Franco y vacaciones no se administran desde el catálogo");
        }
    }

    private static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    private static String nameKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static String slug(String name) {
        String id = fold(name).replaceAll("[^a-z0-9]+", "");
        if (id.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre no sirve para identificar el local");
        }
        if ("franco".equals(id) || "vacaciones".equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Franco y vacaciones no se administran desde el catálogo");
        }
        if (id.length() > 40) {
            id = id.substring(0, 40);
        }
        return id;
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String normalizeColor(String raw) {
        String color = raw == null ? "" : raw.trim();
        if (!color.matches("#[0-9A-Fa-f]{6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El color tiene que ser un hexadecimal, por ejemplo #2A5B8C");
        }
        return color.toUpperCase(Locale.ROOT);
    }

    static String colorSoft(String color) {
        int red = Integer.parseInt(color.substring(1, 3), 16);
        int green = Integer.parseInt(color.substring(3, 5), 16);
        int blue = Integer.parseInt(color.substring(5, 7), 16);
        return String.format("#%02X%02X%02X", towardWhite(red), towardWhite(green), towardWhite(blue));
    }

    private static int towardWhite(int channel) {
        return channel + (255 - channel) * 72 / 100;
    }

    private static short sortOrder(Integer requested, short current) {
        if (requested == null) {
            return current;
        }
        if (requested < 0 || requested > Short.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El orden del local no es válido");
        }
        return requested.shortValue();
    }
}
