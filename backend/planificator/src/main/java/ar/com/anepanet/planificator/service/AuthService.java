package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.security.AuthUser;
import ar.com.anepanet.planificator.security.JwtService;
import ar.com.anepanet.planificator.web.dto.ChangePasswordRequest;
import ar.com.anepanet.planificator.web.dto.LoginRequest;
import ar.com.anepanet.planificator.web.dto.LoginResponse;
import ar.com.anepanet.planificator.web.dto.MeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AuthRepository auth;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;

    public AuthService(
            AuthRepository auth,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService audit) {
        this.auth = auth;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.audit = audit;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        AppUser user = auth.findByUsername(username).filter(AppUser::active).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            audit.recordAs(
                    null,
                    username.isBlank() ? "?" : username,
                    AuditService.ACTION_LOGIN_FAIL,
                    AuditService.TYPE_SESSION,
                    null,
                    "Intento de ingreso fallido");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
        String token = jwtService.createToken(user.id());
        audit.recordAs(
                user.id(),
                user.username(),
                AuditService.ACTION_LOGIN,
                AuditService.TYPE_SESSION,
                user.id().toString(),
                "Ingreso al sistema");
        return new LoginResponse(
                token,
                user.id(),
                user.username(),
                user.displayName(),
                user.roleIds(),
                user.permissions()
        );
    }

    public void logout() {
        AppUser user = currentUser();
        audit.recordAs(
                user.id(),
                user.username(),
                AuditService.ACTION_LOGOUT,
                AuditService.TYPE_SESSION,
                user.id().toString(),
                "Cierre de sesión");
    }

    public void changePassword(ChangePasswordRequest request) {
        AppUser user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña actual incorrecta");
        }
        if (passwordEncoder.matches(request.newPassword(), user.passwordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta a la actual");
        }
        if (!auth.updatePasswordHash(user.id(), passwordEncoder.encode(request.newPassword()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        audit.recordAs(
                user.id(),
                user.username(),
                AuditService.ACTION_UPDATE,
                AuditService.TYPE_USER,
                user.id().toString(),
                "Cambio de contraseña");
    }

    public MeResponse me() {
        AppUser user = currentUser();
        return new MeResponse(
                user.id(),
                user.username(),
                user.displayName(),
                user.roleIds(),
                user.permissions()
        );
    }

    public AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return auth.findById(principal.getId())
                .filter(AppUser::active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no válido"));
    }
}
