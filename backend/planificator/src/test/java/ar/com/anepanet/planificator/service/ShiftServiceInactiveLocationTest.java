package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.ShiftRepository;
import ar.com.anepanet.planificator.security.AuthUser;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.web.dto.CreateShiftRequest;
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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftServiceInactiveLocationTest {

    @Mock ShiftRepository shifts;
    @Mock AuthRepository users;
    @Mock LocationRepository locations;
    @Mock AuditService audit;
    @Mock ApplicationEventPublisher events;
    @Mock VacationQuotaService quotas;

    private ShiftService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new ShiftService(shifts, users, locations, audit, events, quotas);
        userId = UUID.randomUUID();
        AuthUser principal = new AuthUser(
                userId, "supervisor", List.of(new SimpleGrantedAuthority(Permissions.SHIFTS_WRITE)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsInactiveLocation() {
        when(users.findById(userId)).thenReturn(Optional.of(new AppUser(
                userId, "ana", "hash", "Ana", "#112233", null, true, true, false, null,
                OffsetDateTime.now(), OffsetDateTime.now(), List.of(), List.of(), List.of(Permissions.SHIFTS_WRITE))));
        when(locations.findById("lara1")).thenReturn(Optional.of(new Location(
                "lara1", "Lara 1", "#112233", "#D0D5DB", (short) 1, false, true,
                OffsetDateTime.now(), OffsetDateTime.now())));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(new CreateShiftRequest(
                        userId, "lara1", LocalDate.of(2026, 10, 27),
                        LocalTime.of(9, 0), LocalTime.of(15, 0), null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(shifts, never()).insert(any(), any(), any(), any(), any(), any());
        verify(quotas, never()).assertCanLoad(any(), any(), any());
    }
}
