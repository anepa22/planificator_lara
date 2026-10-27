package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.StaffMember;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.service.LocationService;
import ar.com.anepanet.planificator.service.StaffService;
import ar.com.anepanet.planificator.web.dto.CreateStaffRequest;
import ar.com.anepanet.planificator.web.dto.SaveLocationRequest;
import ar.com.anepanet.planificator.web.dto.UpdateLocationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final LocationService locations;
    private final StaffService staff;

    public CatalogController(LocationService locations, StaffService staff) {
        this.locations = locations;
        this.staff = staff;
    }

    @GetMapping("/locations")
    public List<Location> locations() {
        return locations.list();
    }

    @GetMapping("/locations/admin")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public List<Location> manageableLocations() {
        return locations.listManageable();
    }

    @PostMapping("/locations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public Location createLocation(@Valid @RequestBody SaveLocationRequest request) {
        return locations.create(request);
    }

    @PutMapping("/locations/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public Location updateLocation(@PathVariable String id, @Valid @RequestBody UpdateLocationRequest request) {
        return locations.update(id, request);
    }

    @PostMapping("/locations/{id}/baja")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public Location deactivateLocation(@PathVariable String id) {
        return locations.deactivate(id);
    }

    @PostMapping("/locations/{id}/alta")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public Location activateLocation(@PathVariable String id) {
        return locations.activate(id);
    }

    @GetMapping("/staff")
    public List<StaffMember> staff() {
        return staff.list();
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.STAFF_WRITE + "')")
    public StaffMember createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return staff.create(request);
    }

    @DeleteMapping("/staff/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.STAFF_WRITE + "')")
    public void deleteStaff(@PathVariable UUID id) {
        staff.deactivate(id);
    }
}
