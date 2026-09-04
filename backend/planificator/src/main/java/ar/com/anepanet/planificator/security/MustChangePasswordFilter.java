package ar.com.anepanet.planificator.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthUser principal)
                || !principal.mustChangePassword()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (allowedWhileMustChange(request.getMethod(), request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"message\":\"Tenés que cambiar la contraseña\",\"detail\":null}");
    }

    static boolean allowedWhileMustChange(String method, String path) {
        if (method == null || path == null) {
            return false;
        }
        String verb = method.toUpperCase();
        if ("GET".equals(verb) && "/api/auth/me".equals(path)) {
            return true;
        }
        if ("POST".equals(verb) && "/api/auth/change-password".equals(path)) {
            return true;
        }
        if ("POST".equals(verb) && "/api/auth/logout".equals(path)) {
            return true;
        }
        if (!"GET".equals(verb)) {
            return false;
        }
        return "/api/locations".equals(path)
                || "/api/staff".equals(path)
                || "/api/shifts".equals(path)
                || "/api/shifts/summary".equals(path)
                || "/api/holidays".equals(path)
                || "/api/vidrieras".equals(path)
                || "/api/tasks/board".equals(path)
                || (path.startsWith("/api/tasks/") && path.endsWith("/history"));
    }
}
