package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.StaffMember;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.web.dto.CreateStaffRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Personal del planificador. Desde acá se da de alta gente que aparece en la
 * grilla sin poder ingresar al sistema: el login y los roles se habilitan
 * después desde el ABM de usuarios.
 */
@Service
public class StaffService {

    /** Hash imposible de matchear; igual can_login = FALSE ya bloquea el ingreso. */
    private static final String NO_LOGIN_HASH = "!";

    /** El rol Personal es lo que hace que el usuario aparezca en el planificador. */
    private static final String PERSONAL_ROLE = "personal";

    private static final int USERNAME_MAX = 60;

    private final AuthRepository auth;
    private final AuditService audit;

    public StaffService(AuthRepository auth, AuditService audit) {
        this.auth = auth;
        this.audit = audit;
    }

    public List<StaffMember> list() {
        return auth.findActiveStaff();
    }

    public StaffMember create(CreateStaffRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        AppUser created;
        try {
            created = auth.insertUser(
                    uniqueUsername(name),
                    NO_LOGIN_HASH,
                    name,
                    req.color(),
                    false,
                    List.of(PERSONAL_ROLE)
            );
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una persona con ese nombre");
        }
        audit.record(
                AuditService.ACTION_CREATE,
                AuditService.TYPE_STAFF,
                created.id().toString(),
                created.displayName()
        );
        return new StaffMember(created.id(), created.displayName(), created.color());
    }

    public void deactivate(UUID id) {
        AppUser existing = auth.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada"));
        if (!auth.deactivateUser(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Persona no encontrada");
        }
        audit.record(
                AuditService.ACTION_DELETE,
                AuditService.TYPE_STAFF,
                id.toString(),
                existing.displayName()
        );
    }

    /** "Brenda Cappa" -> "brenda.cappa", con sufijo numérico si ya existe. */
    private String uniqueUsername(String displayName) {
        String base = Normalizer.normalize(displayName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.+|\\.+$", "");
        if (base.isEmpty()) {
            base = "personal";
        }
        if (base.length() > USERNAME_MAX) {
            base = base.substring(0, USERNAME_MAX);
        }
        String candidate = base;
        int suffix = 1;
        while (auth.usernameExists(candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }
}
