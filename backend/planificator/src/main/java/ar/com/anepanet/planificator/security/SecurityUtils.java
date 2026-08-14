package ar.com.anepanet.planificator.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<AuthUser> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public static boolean hasAuthority(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (permission.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static void requireAuthority(String permission) {
        if (!hasAuthority(permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permiso: " + permission);
        }
    }

    public static void requireLocationWrite(String locationId) {
        requireAuthority(Permissions.writePermissionForLocation(locationId));
    }
}
