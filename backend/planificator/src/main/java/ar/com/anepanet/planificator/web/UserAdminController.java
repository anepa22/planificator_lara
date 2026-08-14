package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.domain.Permission;
import ar.com.anepanet.planificator.domain.Role;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.service.UserAdminService;
import ar.com.anepanet.planificator.web.dto.CreateUserRequest;
import ar.com.anepanet.planificator.web.dto.UpdateRolePermissionsRequest;
import ar.com.anepanet.planificator.web.dto.UpdateUserRequest;
import ar.com.anepanet.planificator.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UserAdminController {

    private final UserAdminService users;

    public UserAdminController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public List<UserResponse> listUsers() {
        return users.listUsers();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return users.createUser(request);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return users.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "')")
    public void deleteUser(@PathVariable UUID id) {
        users.deleteUser(id);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('" + Permissions.USERS_MANAGE + "') or hasAuthority('" + Permissions.ROLES_MANAGE + "')")
    public List<Role> listRoles() {
        return users.listRoles();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('" + Permissions.ROLES_MANAGE + "')")
    public List<Permission> listPermissions() {
        return users.listPermissions();
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('" + Permissions.ROLES_MANAGE + "')")
    public Role updateRolePermissions(
            @PathVariable String id,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return users.updateRolePermissions(id, request);
    }
}
