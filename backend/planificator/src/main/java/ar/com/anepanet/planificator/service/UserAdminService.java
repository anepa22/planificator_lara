package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Permission;
import ar.com.anepanet.planificator.domain.Role;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.web.dto.CreateUserRequest;
import ar.com.anepanet.planificator.web.dto.UpdateRolePermissionsRequest;
import ar.com.anepanet.planificator.web.dto.UpdateUserRequest;
import ar.com.anepanet.planificator.web.dto.UserResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private static final int PASSWORD_MIN = 6;

    /** Marca para cuentas sin login; ningún hash real puede coincidir. */
    private static final String NO_LOGIN_HASH = "!";

    private final AuthRepository auth;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserAdminService(
            AuthRepository auth,
            PasswordEncoder passwordEncoder,
            AuditService audit) {
        this.auth = auth;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    public List<UserResponse> listUsers() {
        return auth.findAllUsers().stream().map(this::toResponse).toList();
    }

    public UserResponse createUser(CreateUserRequest req) {
        validateRoles(req.roleIds());
        String username = requireUsername(req.username());
        boolean canLogin = req.canLogin() == null || req.canLogin();
        String hash = NO_LOGIN_HASH;
        if (canLogin) {
            requirePassword(req.password());
            hash = passwordEncoder.encode(req.password());
        }
        try {
            AppUser user = auth.insertUser(
                    username,
                    hash,
                    req.displayName(),
                    req.color(),
                    canLogin,
                    req.roleIds()
            );
            audit.record(
                    AuditService.ACTION_CREATE,
                    AuditService.TYPE_USER,
                    user.id().toString(),
                    user.username()
                            + " (" + String.join(", ", req.roleIds()) + ")"
                            + (canLogin ? "" : " · sin login")
            );
            return toResponse(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
        }
    }

    public UserResponse updateUser(UUID id, UpdateUserRequest req) {
        validateRoles(req.roleIds());
        AppUser existing = auth.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        boolean canLogin = req.canLogin() == null ? existing.canLogin() : req.canLogin();

        String hash = null;
        boolean passwordChanged = false;
        if (req.password() != null && !req.password().isBlank()) {
            requirePassword(req.password());
            hash = passwordEncoder.encode(req.password());
            passwordChanged = true;
        }
        if (canLogin && !existing.canLogin() && !passwordChanged) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Para habilitar el ingreso hay que definir una contraseña");
        }
        if (!canLogin) {
            hash = NO_LOGIN_HASH;
            passwordChanged = false;
        }

        AppUser user = auth.updateUser(
                        id,
                        req.displayName(),
                        req.active(),
                        hash,
                        req.color(),
                        canLogin,
                        passwordChanged && canLogin ? Boolean.TRUE : null,
                        req.roleIds())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        audit.record(
                AuditService.ACTION_UPDATE,
                AuditService.TYPE_USER,
                user.id().toString(),
                user.username()
                        + " · roles=[" + String.join(", ", req.roleIds()) + "]"
                        + " · activo=" + req.active()
                        + " · login=" + canLogin
                        + (passwordChanged ? " · contraseña cambiada" : "")
        );
        return toResponse(user);
    }

    public void deleteUser(UUID id) {
        AppUser current = auth.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if ("admin".equalsIgnoreCase(current.username())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede eliminar el usuario admin");
        }
        if (!auth.deleteUser(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        audit.record(
                AuditService.ACTION_DELETE,
                AuditService.TYPE_USER,
                id.toString(),
                current.username()
        );
    }

    public List<Role> listRoles() {
        return auth.findAllRoles().stream()
                .filter(r -> !"viewer".equals(r.id()))
                .toList();
    }

    public List<Permission> listPermissions() {
        return auth.findAllPermissions().stream()
                .filter(p -> !"plan:read".equals(p.code()) && !"plan_read".equals(p.id()))
                .toList();
    }

    public Role updateRolePermissions(String roleId, UpdateRolePermissionsRequest req) {
        if (auth.findRoleById(roleId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado");
        }
        Set<String> known = new HashSet<>();
        auth.findAllPermissions().forEach(p -> known.add(p.code()));
        for (String code : req.permissionCodes()) {
            if (!known.contains(code)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permiso desconocido: " + code);
            }
        }
        auth.replaceRolePermissions(roleId, req.permissionCodes());
        Role role = auth.findRoleById(roleId).orElseThrow();
        String perms = req.permissionCodes().stream().sorted().collect(Collectors.joining(", "));
        audit.record(
                AuditService.ACTION_UPDATE,
                AuditService.TYPE_ROLE,
                roleId,
                "Permisos del rol " + roleId + ": [" + perms + "]"
        );
        return role;
    }

    private static String requireUsername(String username) {
        String value = username == null ? "" : username.trim().toLowerCase();
        if (value.length() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El nombre de usuario es obligatorio");
        }
        if (!value.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de usuario solo puede tener letras, números, punto, guion o guion bajo");
        }
        return value;
    }

    private void requirePassword(String password) {
        if (password == null || password.isBlank() || password.length() < PASSWORD_MIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres");
        }
    }

    private void validateRoles(List<String> roleIds) {
        if (roleIds == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles inválidos");
        }
        for (String roleId : roleIds) {
            if ("viewer".equals(roleId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol Solo lectura ya no existe");
            }
            if (!auth.roleExists(roleId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol desconocido: " + roleId);
            }
        }
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.displayName(),
                user.color(),
                user.active(),
                user.canLogin(),
                user.roleIds(),
                user.permissions()
        );
    }
}
