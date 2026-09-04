package ar.com.anepanet.planificator.security;

import ar.com.anepanet.planificator.domain.UserPrincipal;
import ar.com.anepanet.planificator.repository.AuthRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final AuthRepository auth;

    public JwtAuthFilter(JwtService jwtService, AuthRepository auth) {
        this.jwtService = jwtService;
        this.auth = auth;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            try {
                Claims claims = jwtService.parse(token);
                UUID userId = UUID.fromString(claims.getSubject());
                UserPrincipal user = auth.findPrincipalById(userId)
                        .filter(UserPrincipal::active)
                        .orElse(null);
                if (user != null) {
                    Collection<SimpleGrantedAuthority> authorities = user.permissions().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    var principal = new AuthUser(
                            user.id(), user.username(), authorities, user.mustChangePassword());
                    var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception ex) {
                log.debug("JWT inválido o usuario no válido: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
