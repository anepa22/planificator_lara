package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.security.AuthUser;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.web.dto.SaveLocationRequest;
import ar.com.anepanet.planificator.web.dto.UpdateLocationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock LocationRepository locations;
    @Mock AuditService audit;

    private LocationService service;

    @BeforeEach
    void setUp() {
        service = new LocationService(locations, audit);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminListIncludesInactiveSalesLocationAndKeepsIdOnRename() {
        login(Permissions.LOCATIONS_MANAGE);
        Location inactive = location("lara1", "Lara 1", false, false);
        when(locations.findAll()).thenReturn(List.of(
                inactive,
                location("franco", "Franco", true, false),
                location("vacaciones", "Vacaciones", true, false)));

        List<Location> manageable = service.listManageable();

        assertEquals(List.of(inactive), manageable);

        Location renamed = location("lara1", "Lara Uno", false, false);
        when(locations.findById("lara1")).thenReturn(Optional.of(inactive), Optional.of(renamed));
        when(locations.nameTaken("lara uno", "lara1")).thenReturn(false);
        when(locations.update(eq("lara1"), eq("Lara Uno"), eq("#112233"), anyString(), eq((short) 1), eq(false), eq(false)))
                .thenReturn(1);

        Location result = service.update("lara1", new UpdateLocationRequest("Lara Uno", "#112233", false, null));

        assertEquals("lara1", result.id());
        assertEquals("Lara Uno", result.name());
        verify(locations).update(eq("lara1"), eq("Lara Uno"), eq("#112233"), anyString(), eq((short) 1), eq(false), eq(false));
    }

    @Test
    void createRejectsEmptyNameDuplicateNameAndReservedName() {
        login(Permissions.LOCATIONS_MANAGE);

        ResponseStatusException empty = assertThrows(
                ResponseStatusException.class,
                () -> service.create(new SaveLocationRequest("   ", "#112233", false)));
        assertEquals(HttpStatus.BAD_REQUEST, empty.getStatusCode());

        when(locations.nameTaken("lara 1", "")).thenReturn(true);
        ResponseStatusException duplicate = assertThrows(
                ResponseStatusException.class,
                () -> service.create(new SaveLocationRequest("lara 1", "#112233", true)));
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());

        ResponseStatusException reserved = assertThrows(
                ResponseStatusException.class,
                () -> service.create(new SaveLocationRequest("Vacaciones", "#112233", false)));
        assertEquals(HttpStatus.BAD_REQUEST, reserved.getStatusCode());
        verify(locations, never()).insert(anyString(), anyString(), anyString(), anyString(), anyShort(), anyBoolean());
    }

    @Test
    void deactivateAndActivateAreAuditedAndFrancoIsRejected() {
        login(Permissions.LOCATIONS_MANAGE);
        Location active = location("lara3", "Lara 3", true, true);
        Location inactive = location("lara3", "Lara 3", false, true);
        when(locations.findById("lara3")).thenReturn(Optional.of(active), Optional.of(inactive), Optional.of(inactive), Optional.of(active));
        when(locations.update(eq("lara3"), eq("Lara 3"), eq("#112233"), anyString(), eq((short) 1), eq(false), eq(true)))
                .thenReturn(1);
        when(locations.update(eq("lara3"), eq("Lara 3"), eq("#112233"), anyString(), eq((short) 1), eq(true), eq(true)))
                .thenReturn(1);

        assertEquals(false, service.deactivate("lara3").active());
        verify(audit).record(AuditService.ACTION_DELETE, AuditService.TYPE_LOCATION, "lara3", "Baja de local Lara 3");

        assertEquals(true, service.activate("lara3").active());
        verify(audit).record(AuditService.ACTION_UPDATE, AuditService.TYPE_LOCATION, "lara3", "Alta de local Lara 3");

        ResponseStatusException franco = assertThrows(
                ResponseStatusException.class, () -> service.deactivate("franco"));
        assertEquals(HttpStatus.BAD_REQUEST, franco.getStatusCode());
    }

    @Test
    void usersManageAloneDoesNotManageLocations() {
        login(Permissions.USERS_MANAGE);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.listManageable());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(locations, never()).findAll();
    }

    @Test
    void withoutPermissionRespondsForbidden() {
        login(Permissions.SHIFTS_WRITE);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class, () -> service.listManageable());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(locations, never()).findAll();
    }

    @Test
    void createStoresActiveLocation() {
        login(Permissions.LOCATIONS_MANAGE);
        when(locations.idExists("lara3")).thenReturn(false);
        when(locations.nameTaken("lara 3", "")).thenReturn(false);
        when(locations.nextSortOrder()).thenReturn((short) 4);
        Location created = location("lara3", "Lara 3", true, true);
        when(locations.findById("lara3")).thenReturn(Optional.of(created));

        Location result = service.create(new SaveLocationRequest("Lara 3", "#2A5B8C", true));

        assertTrue(result.active());
        assertTrue(result.supportsVidriera());
        verify(locations).insert(eq("lara3"), eq("Lara 3"), eq("#2A5B8C"), anyString(), eq((short) 4), eq(true));
        verify(audit).record(AuditService.ACTION_CREATE, AuditService.TYPE_LOCATION, "lara3", "Alta de local Lara 3");
    }

    @Test
    void createAndActivateRejectWhenEightSalesLocationsAreActive() {
        login(Permissions.LOCATIONS_MANAGE);
        when(locations.countActiveSales()).thenReturn(LocationService.MAX_ACTIVE_SALES);

        ResponseStatusException created = assertThrows(
                ResponseStatusException.class,
                () -> service.create(new SaveLocationRequest("Lara 9", "#112233", false)));
        assertEquals(HttpStatus.CONFLICT, created.getStatusCode());
        verify(locations, never()).insert(anyString(), anyString(), anyString(), anyString(), anyShort(), anyBoolean());

        Location inactive = location("lara1", "Lara 1", false, false);
        when(locations.findById("lara1")).thenReturn(Optional.of(inactive));
        ResponseStatusException activated = assertThrows(
                ResponseStatusException.class, () -> service.activate("lara1"));
        assertEquals(HttpStatus.CONFLICT, activated.getStatusCode());
        verify(locations, never()).update(anyString(), anyString(), anyString(), anyString(), anyShort(), anyBoolean(), anyBoolean());
    }

    private void login(String permission) {
        AuthUser principal = new AuthUser(
                UUID.randomUUID(), "admin", List.of(new SimpleGrantedAuthority(permission)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static Location location(String id, String name, boolean active, boolean vidriera) {
        OffsetDateTime now = OffsetDateTime.parse("2026-10-27T12:00:00Z");
        return new Location(id, name, "#112233", "#D0D5DB", (short) 1, active, vidriera, now, now);
    }
}
