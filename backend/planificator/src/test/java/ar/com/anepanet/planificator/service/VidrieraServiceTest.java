package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.Vidriera;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.VidrieraRepository;
import ar.com.anepanet.planificator.security.AuthUser;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.web.dto.UpsertVidrieraRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VidrieraServiceTest {

    @Mock VidrieraRepository vidrieras;
    @Mock LocationRepository locations;
    @Mock AuditService audit;
    @Mock ApplicationEventPublisher events;

    private VidrieraService service;

    @BeforeEach
    void setUp() {
        service = new VidrieraService(vidrieras, locations, audit, events);
        AuthUser principal = new AuthUser(
                UUID.randomUUID(), "supervisor", List.of(new SimpleGrantedAuthority(Permissions.SHIFTS_WRITE)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void newVidrieraRejectsInactiveOrDisabledLocationAndSavedMarkStaysReadable() {
        LocalDate day = LocalDate.of(2026, 10, 27);
        when(locations.findById("lara1")).thenReturn(Optional.of(location(false, true)));

        ResponseStatusException inactive = assertThrows(
                ResponseStatusException.class,
                () -> service.upsert(new UpsertVidrieraRequest("lara1", day)));
        assertEquals(HttpStatus.BAD_REQUEST, inactive.getStatusCode());

        when(locations.findById("lara1")).thenReturn(Optional.of(location(true, false)));
        ResponseStatusException disabled = assertThrows(
                ResponseStatusException.class,
                () -> service.upsert(new UpsertVidrieraRequest("lara1", day)));
        assertEquals(HttpStatus.BAD_REQUEST, disabled.getStatusCode());

        when(vidrieras.findByDateRange(day, day)).thenReturn(List.of(new Vidriera("lara1", "Lara 1", day)));
        assertEquals("Lara 1", service.byRange(day, day).get(0).locationName());
    }

    private static Location location(boolean active, boolean vidriera) {
        OffsetDateTime now = OffsetDateTime.parse("2026-10-27T12:00:00Z");
        return new Location("lara1", "Lara 1", "#112233", "#D0D5DB", (short) 1, active, vidriera, now, now);
    }
}
