package ar.com.anepanet.planificator.repository;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Permission;
import ar.com.anepanet.planificator.domain.Role;
import ar.com.anepanet.planificator.domain.StaffMember;
import ar.com.anepanet.planificator.domain.UserPrincipal;
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

    private static final String USER_COLUMNS = """
            id, username, password_hash, display_name, color,
            is_active, can_login, must_change_password, created_at, updated_at
            """;

    private final JdbcClient jdbc;

    public AuthRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUser> findByUsername(String username) {
        return jdbc.sql("""
                SELECT %s
                FROM app_users
                WHERE lower(username) = lower(:username)
                """.formatted(USER_COLUMNS))
                .param("username", username)
                .query(this::mapUserBase)
                .optional()
                .map(this::withRolesAndPermissions);
    }

    public Optional<AppUser> findById(UUID id) {
        return jdbc.sql("""
                SELECT %s
                FROM app_users
                WHERE id = :id
                """.formatted(USER_COLUMNS))
                .param("id", id)
                .query(this::mapUserBase)
                .optional()
                .map(this::withRolesAndPermissions);
    }

    /**
     * Proyección ligera para autenticar requests: evita cargar contraseña,
     * roles y metadatos, y obtiene todos los permisos en una sola consulta.
     */
    public Optional<UserPrincipal> findPrincipalById(UUID id) {
        List<PrincipalRow> rows = jdbc.sql("""
                SELECT u.id, u.username, u.is_active, u.must_change_password, p.code
                FROM app_users u
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                LEFT JOIN role_permissions rp ON rp.role_id = ur.role_id
                LEFT JOIN permissions p ON p.id = rp.permission_id
                WHERE u.id = :id
                ORDER BY p.code
                """)
                .param("id", id)
                .query((rs, n) -> new PrincipalRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        rs.getBoolean("is_active"),
                        rs.getBoolean("must_change_password"),
                        rs.getString("code")))
                .list();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        PrincipalRow user = rows.get(0);
        List<String> permissions = rows.stream()
                .map(PrincipalRow::permission)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return Optional.of(new UserPrincipal(
                user.id(),
                user.username(),
                user.active(),
                user.mustChangePassword(),
                permissions));
    }

    public List<AppUser> findAllUsers() {
        List<AppUser> base = jdbc.sql("""
                SELECT %s
                FROM app_users
                ORDER BY display_name
                """.formatted(USER_COLUMNS))
                .query(this::mapUserBase)
                .list();
        if (base.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<String>> rolesByUser = groupedStrings("""
                SELECT user_id, role_id AS value
                FROM user_roles
                ORDER BY user_id, role_id
                """);
        Map<UUID, List<String>> permissionsByUser = groupedStrings("""
                SELECT DISTINCT ur.user_id, p.code AS value
                FROM user_roles ur
                JOIN role_permissions rp ON rp.role_id = ur.role_id
                JOIN permissions p ON p.id = rp.permission_id
                ORDER BY ur.user_id, p.code
                """);
        return base.stream()
                .map(user -> withRolesAndPermissions(
                        user,
                        rolesByUser.getOrDefault(user.id(), List.of()),
                        permissionsByUser.getOrDefault(user.id(), List.of())))
                .toList();
    }

    /**
     * Personal visible en el planificador: usuarios activos con rol Personal,
     * tengan habilitado el ingreso o no. Las cuentas administrativas quedan
     * afuera de la grilla.
     */
    public List<StaffMember> findActiveStaff() {
        return jdbc.sql("""
                SELECT u.id, u.display_name, u.color
                FROM app_users u
                JOIN user_roles ur ON ur.user_id = u.id AND ur.role_id = 'personal'
                WHERE u.is_active = TRUE
                ORDER BY u.display_name
                """)
                .query((rs, n) -> new StaffMember(
                        rs.getObject("id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("color")
                ))
                .list();
    }

    public boolean usernameExists(String username) {
        Integer n = jdbc.sql("SELECT COUNT(*) FROM app_users WHERE lower(username) = lower(:username)")
                .param("username", username)
                .query(Integer.class)
                .single();
        return n != null && n > 0;
    }

    public AppUser insertUser(
            String username,
            String passwordHash,
            String displayName,
            String color,
            boolean canLogin,
            List<String> roleIds) {
        AppUser created = jdbc.sql("""
                INSERT INTO app_users (
                    username, password_hash, display_name, color, can_login, must_change_password
                )
                VALUES (
                    :username, :passwordHash, :displayName, :color, :canLogin, :mustChangePassword
                )
                RETURNING %s
                """.formatted(USER_COLUMNS))
                .param("username", username.trim())
                .param("passwordHash", passwordHash)
                .param("displayName", displayName.trim())
                .param("color", color)
                .param("canLogin", canLogin)
                .param("mustChangePassword", canLogin)
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
            String color,
            boolean canLogin,
            Boolean mustChangePassword,
            List<String> roleIds) {
        Optional<AppUser> existing = findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        jdbc.sql("""
                UPDATE app_users
                SET display_name = :displayName,
                    is_active = :active,
                    color = :color,
                    can_login = :canLogin,
                    password_hash = COALESCE(:passwordHash, password_hash),
                    must_change_password = COALESCE(:mustChangePassword, must_change_password),
                    updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("displayName", displayName.trim())
                .param("active", active)
                .param("color", color)
                .param("canLogin", canLogin)
                .param("passwordHash", passwordHash)
                .param("mustChangePassword", mustChangePassword)
                .update();
        if (roleIds != null) {
            replaceUserRoles(id, roleIds);
        }
        return findById(id);
    }

    public boolean updatePasswordHash(UUID id, String passwordHash) {
        return jdbc.sql("""
                UPDATE app_users
                SET password_hash = :passwordHash,
                    must_change_password = FALSE,
                    updated_at = NOW()
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

    /** Baja lógica: el usuario deja de aparecer en el planificador y no puede ingresar. */
    public boolean deactivateUser(UUID id) {
        return jdbc.sql("""
                UPDATE app_users
                SET is_active = FALSE, updated_at = NOW()
                WHERE id = :id AND is_active = TRUE
                """)
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
        Optional<Role> role = jdbc.sql("""
                SELECT id, code, name
                FROM roles
                WHERE id = :id
                """)
                .param("id", id)
                .query((rs, n) -> new Role(
                        rs.getString("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        List.of()))
                .optional();
        if (role.isEmpty()) {
            return Optional.empty();
        }
        List<String> permissions = jdbc.sql("""
                SELECT p.code
                FROM role_permissions rp
                JOIN permissions p ON p.id = rp.permission_id
                WHERE rp.role_id = :id
                ORDER BY p.code
                """)
                .param("id", id)
                .query(String.class)
                .list();
        Role base = role.get();
        return Optional.of(new Role(base.id(), base.code(), base.name(), permissions));
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
        return withRolesAndPermissions(base, roleIds, permissions);
    }

    private Map<UUID, List<String>> groupedStrings(String sql) {
        Map<UUID, List<String>> grouped = new LinkedHashMap<>();
        jdbc.sql(sql)
                .query((rs, n) -> Map.entry(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("value")))
                .list()
                .forEach(row -> grouped
                        .computeIfAbsent(row.getKey(), ignored -> new ArrayList<>())
                        .add(row.getValue()));
        return grouped;
    }

    private AppUser withRolesAndPermissions(
            AppUser base,
            List<String> roleIds,
            List<String> permissions) {
        return new AppUser(
                base.id(),
                base.username(),
                base.passwordHash(),
                base.displayName(),
                base.color(),
                base.active(),
                base.canLogin(),
                base.mustChangePassword(),
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
                rs.getString("color"),
                rs.getBoolean("is_active"),
                rs.getBoolean("can_login"),
                rs.getBoolean("must_change_password"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                List.of(),
                List.of()
        );
    }

    private record PrincipalRow(
            UUID id,
            String username,
            boolean active,
            boolean mustChangePassword,
            String permission) {}
}
