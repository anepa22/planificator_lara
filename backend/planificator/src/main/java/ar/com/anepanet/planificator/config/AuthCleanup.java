package ar.com.anepanet.planificator.config;

import ar.com.anepanet.planificator.security.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Mantenimiento idempotente de auth al arrancar:
 * quita viewer/plan:read legados y asegura permisos lunch:manage / audit:read.
 */
@Component
public class AuthCleanup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthCleanup.class);

    private final JdbcClient jdbc;

    public AuthCleanup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.sql("""
                    INSERT INTO user_roles (user_id, role_id)
                    SELECT ur.user_id, 'editor'
                    FROM user_roles ur
                    WHERE ur.role_id = 'viewer'
                      AND NOT EXISTS (
                          SELECT 1 FROM user_roles x
                          WHERE x.user_id = ur.user_id AND x.role_id = 'editor'
                      )
                    ON CONFLICT DO NOTHING
                    """).update();
            int userRoles = jdbc.sql("DELETE FROM user_roles WHERE role_id = 'viewer'").update();
            int rolePerms = jdbc.sql("""
                    DELETE FROM role_permissions
                    WHERE role_id = 'viewer' OR permission_id = 'plan_read'
                    """).update();
            int roles = jdbc.sql("DELETE FROM roles WHERE id = 'viewer'").update();
            int perms = jdbc.sql("DELETE FROM permissions WHERE id = 'plan_read'").update();
            if (userRoles + rolePerms + roles + perms > 0) {
                log.info("Auth cleanup: removed legacy viewer/plan:read (roles={}, perms={}, links={})",
                        roles, perms, userRoles + rolePerms);
            }

            jdbc.sql("""
                    ALTER TABLE app_users
                    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN
                    """).update();
            jdbc.sql("""
                    UPDATE app_users
                    SET must_change_password = FALSE
                    WHERE must_change_password IS NULL
                    """).update();
            jdbc.sql("""
                    ALTER TABLE app_users
                    ALTER COLUMN must_change_password SET DEFAULT TRUE
                    """).update();
            jdbc.sql("""
                    ALTER TABLE app_users
                    ALTER COLUMN must_change_password SET NOT NULL
                    """).update();

            ensurePermission("lunch_manage", Permissions.LUNCH_MANAGE, "Configurar almuerzo");
            jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id) VALUES
                        ('admin', 'lunch_manage'),
                        ('editor', 'lunch_manage')
                    ON CONFLICT DO NOTHING
                    """).update();

            ensurePermission("audit_read", Permissions.AUDIT_READ, "Ver bitácora");
            jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id) VALUES
                        ('admin', 'audit_read')
                    ON CONFLICT DO NOTHING
                    """).update();

            ensurePermission("tasks_write", Permissions.TASKS_WRITE, "Tomar y mover tareas propias");
            ensurePermission("tasks_manage", Permissions.TASKS_MANAGE, "Administrar todas las tareas");
            ensurePermission("tasks_history", Permissions.TASKS_HISTORY, "Ver historial de movimientos de tareas");
            ensurePermission("tasks_retention", Permissions.TASKS_RETENTION, "Configurar retiro de tareas verificadas");
            jdbc.sql("""
                    INSERT INTO roles (id, code, name) VALUES
                        ('personal', 'personal', 'Personal')
                    ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, name = EXCLUDED.name
                    """).update();
            jdbc.sql("UPDATE roles SET name = 'Supervisor' WHERE id = 'editor'").update();
            jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id) VALUES
                        ('personal', 'tasks_write'),
                        ('editor', 'tasks_write'),
                        ('editor', 'tasks_manage'),
                        ('editor', 'tasks_history'),
                        ('editor', 'tasks_retention'),
                        ('admin', 'tasks_write'),
                        ('admin', 'tasks_manage'),
                        ('admin', 'tasks_history'),
                        ('admin', 'tasks_retention')
                    ON CONFLICT DO NOTHING
                    """).update();

            // people:write pasó a staff:write al unificar personal y usuarios
            ensurePermission("staff_write", Permissions.STAFF_WRITE, "Gestionar personal");
            jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id)
                    SELECT rp.role_id, 'staff_write'
                    FROM role_permissions rp
                    WHERE rp.permission_id = 'people_write'
                    ON CONFLICT DO NOTHING
                    """).update();
            jdbc.sql("""
                    INSERT INTO role_permissions (role_id, permission_id) VALUES
                        ('admin', 'staff_write'),
                        ('editor', 'staff_write')
                    ON CONFLICT DO NOTHING
                    """).update();
            jdbc.sql("DELETE FROM role_permissions WHERE permission_id = 'people_write'").update();
            jdbc.sql("DELETE FROM permissions WHERE id = 'people_write'").update();
        } catch (Exception ex) {
            log.warn("Auth cleanup skipped: {}", ex.getMessage());
        }
    }

    private void ensurePermission(String id, String code, String name) {
        jdbc.sql("""
                INSERT INTO permissions (id, code, name) VALUES
                    (:id, :code, :name)
                ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, name = EXCLUDED.name
                """)
                .param("id", id)
                .param("code", code)
                .param("name", name)
                .update();
    }
}
