package ar.com.anepanet.planificator.repository;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Permission;
import ar.com.anepanet.planificator.domain.Role;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AuthRepository {

    private final JdbcClient jdbc;

    public AuthRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUser> findByUsername(String username) {
        return jdbc.sql("""
                SELECT id, username, password_hash, display_name, person_id,
                       is_active, created_at, updated_at
                FROM app_users
                WHERE lower(username) = lower(:username)
                """)
                .param("username", username)
                .query(this::mapUserBase)
                .optional()
                .map(this::withRolesAndPermissions);
    }

    public Optional<AppUser> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, username, password_hash, display_name, person_id,
                       is_active, created_at, updated_at
                FROM app_users
                WHERE id = :id
                """)
                .param("id", id)
                .query(this::mapUserBase)
                .optional()
                .map(this::withRolesAndPermissions);
    }

    public List<AppUser> findAllUsers() {
        List<AppUser> base = jdbc.sql("""
                SELECT id, username, password_hash, display_name, person_id,
                       is_active, created_at, updated_at
                FROM app_users
                ORDER BY username
                """)
                .query(this::mapUserBase)
                .list();
        return base.stream().map(this::withRolesAndPermissions).toList();
    }

    public AppUser insertUser(
            String username,
            String passwordHash,
            String displayName,
            UUID personId,
            List<String> roleIds) {
        AppUser created = jdbc.sql("""
                INSERT INTO app_users (username, password_hash, display_name, person_id)
                VALUES (:username, :passwordHash, :displayName, :personId)
                RETURNING id, username, password_hash, display_name, person_id,
                          is_active, created_at, updated_at
                """)
                .param("username", username.trim())
                .param("passwordHash", passwordHash)
                .param("displayName", displayName.trim())
                .param("personId", personId)
                .query(this::mapUserBase)
                .single();
        replaceUserRoles(created.id(), roleIds);
        return withRolesAndPermissions(created);
    }

    public Optional<AppUser> updateUser(
            UUID id,
            String displayName,
            Boolean active,
            String passwordHash,
            UUID personId,
            List<String> roleIds) {
        Optional<AppUser> existing = findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        jdbc.sql("""
                UPDATE app_users
                SET display_name = :displayName,
                    is_active = :active,
                    person_id = :personId,
                    password_hash = COALESCE(:passwordHash, password_hash),
                    updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("displayName", displayName.trim())
                .param("active", active)
                .param("passwordHash", passwordHash)
                .param("personId", personId)
                .update();
        if (roleIds != null) {
            replaceUserRoles(id, roleIds);
        }
        return findById(id);
    }

    public boolean updatePasswordHash(UUID id, String passwordHash) {
        return jdbc.sql("""
                UPDATE app_users
                SET password_hash = :passwordHash, updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("passwordHash", passwordHash)
                .update() > 0;
    }

    public boolean deleteUser(UUID id) {
        return jdbc.sql("DELETE FROM app_users WHERE id = :id")
                .param("id", id)
                .update() > 0;
    }

    public List<Role> findAllRoles() {
        List<Role> roles = jdbc.sql("""
                SELECT id, code, name FROM roles ORDER BY code
                """)
                .query((rs, n) -> new Role(
                        rs.getString("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        List.of()
                ))
                .list();
        Map<String, List<String>> byRole = new LinkedHashMap<>();
        for (var row : jdbc.sql("""
                SELECT rp.role_id, p.code
                FROM role_permissions rp
                JOIN permissions p ON p.id = rp.permission_id
                ORDER BY p.code
                """)
                .query((rs, n) -> Map.entry(rs.getString("role_id"), rs.getString("code")))
                .list()) {
            byRole.computeIfAbsent(row.getKey(), k -> new ArrayList<>()).add(row.getValue());
        }
        return roles.stream()
                .map(r -> new Role(r.id(), r.code(), r.name(),
                        List.copyOf(byRole.getOrDefault(r.id(), List.of()))))
                .toList();
    }

    public Optional<Role> findRoleById(String id) {
        return findAllRoles().stream().filter(r -> r.id().equals(id)).findFirst();
    }

    public void replaceRolePermissions(String roleId, List<String> permissionCodes) {
        jdbc.sql("DELETE FROM role_permissions WHERE role_id = :roleId")
                .param("roleId", roleId)
                .update();
        for (String code : permissionCodes) {
            jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id)
                    SELECT :roleId, id FROM permissions WHERE code = :code
                    """)
                    .param("roleId", roleId)
                    .param("code", code)
                    .update();
        }
    }

    public List<Permission> findAllPermissions() {
        return jdbc.sql("""
                SELECT id, code, name FROM permissions ORDER BY code
                """)
                .query((rs, n) -> new Permission(
                        rs.getString("id"),
                        rs.getString("code"),
                        rs.getString("name")
                ))
                .list();
    }

    public boolean roleExists(String roleId) {
        Integer n = jdbc.sql("SELECT COUNT(*) FROM roles WHERE id = :id")
                .param("id", roleId)
                .query(Integer.class)
                .single();
        return n != null && n > 0;
    }

    private void replaceUserRoles(UUID userId, List<String> roleIds) {
        jdbc.sql("DELETE FROM user_roles WHERE user_id = :userId")
                .param("userId", userId)
                .update();
        for (String roleId : roleIds) {
            jdbc.sql("""
                    INSERT INTO user_roles (user_id, role_id)
                    VALUES (:userId, :roleId)
                    """)
                    .param("userId", userId)
                    .param("roleId", roleId)
                    .update();
        }
    }

    private AppUser withRolesAndPermissions(AppUser base) {
        List<String> roleIds = jdbc.sql("""
                SELECT role_id FROM user_roles WHERE user_id = :userId ORDER BY role_id
                """)
                .param("userId", base.id())
                .query(String.class)
                .list();
        List<String> permissions = jdbc.sql("""
                SELECT DISTINCT p.code
                FROM user_roles ur
                JOIN role_permissions rp ON rp.role_id = ur.role_id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE ur.user_id = :userId
                ORDER BY p.code
                """)
                .param("userId", base.id())
                .query(String.class)
                .list();
        return new AppUser(
                base.id(),
                base.username(),
                base.passwordHash(),
                base.displayName(),
                base.personId(),
                base.active(),
                base.createdAt(),
                base.updatedAt(),
                roleIds,
                permissions
        );
    }

    private AppUser mapUserBase(ResultSet rs, int rowNum) throws SQLException {
        return new AppUser(
                rs.getObject("id", UUID.class),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                rs.getObject("person_id", UUID.class),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                List.of(),
                List.of()
        );
    }
}
