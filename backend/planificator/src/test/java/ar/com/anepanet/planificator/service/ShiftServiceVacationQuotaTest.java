package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.ShiftRepository;
import ar.com.anepanet.planificator.security.AuthUser;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.web.dto.AssignVacationsRequest;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftServiceVacationQuotaTest {

    @Mock private ShiftRepository shifts;
    @Mock private AuthRepository users;
    @Mock private LocationRepository locations;
    @Mock private AuditService audit;
    @Mock private ApplicationEventPublisher events;
    @Mock private VacationQuotaService quotas;

    private ShiftService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ShiftService(shifts, users, locations, audit, events, quotas);
        AuthUser principal = new AuthUser(
                userId, "admin", List.of(new SimpleGrantedAuthority(Permissions.VACATIONS_WRITE)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void overQuotaAssignsNothing() {
        when(locations.findById(Permissions.VACATION_LOCATION_ID)).thenReturn(Optional.of(vacationLocation()));
        when(users.findById(userId)).thenReturn(Optional.of(person()));
        doThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A Ana le quedan 4 días en el período del 1 oct 2026 – 30 sep 2027."))
                .when(quotas).assertAll(any(), any(), any(), any());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.assignVacations(
                new AssignVacationsRequest(
                        List.of(userId),
                        LocalDate.of(2026, 12, 1),
                        LocalDate.of(2026, 12, 5),
                        LocalTime.of(8, 0),
                        LocalTime.of(22, 0),
                        List.of())));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(shifts, never()).insert(any(), any(), any(), any(), any(), any());
        verify(shifts, never()).delete(any());
    }

    @Test
    void missingHireDateAssignsNothing() {
        when(locations.findById(Permissions.VACATION_LOCATION_ID)).thenReturn(Optional.of(vacationLocation()));
        when(users.findById(userId)).thenReturn(Optional.of(person()));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la fecha de ingreso de Ana."))
                .when(quotas).assertAll(any(), any(), any(), any());

        assertThrows(ResponseStatusException.class, () -> service.assignVacations(
                new AssignVacationsRequest(
                        List.of(userId),
                        LocalDate.of(2026, 10, 2),
                        LocalDate.of(2026, 10, 2),
                        LocalTime.of(8, 0),
                        LocalTime.of(22, 0),
                        List.of())));

        verify(shifts, never()).insert(any(), any(), any(), any(), any(), any());
    }

    private AppUser person() {
        return new AppUser(
                userId, "ana", "hash", "Ana", "#123456", null, true, true, false,
                LocalDate.of(2020, 1, 1),
                OffsetDateTime.now(), OffsetDateTime.now(),
                List.of("personal"), List.of(), List.of(Permissions.VACATIONS_WRITE));
    }

    private Location vacationLocation() {
        return new Location(
                Permissions.VACATION_LOCATION_ID, "Vacaciones", "#ccc", "#eee",
                (short) 1, true, false, OffsetDateTime.now(), OffsetDateTime.now());
    }
}
