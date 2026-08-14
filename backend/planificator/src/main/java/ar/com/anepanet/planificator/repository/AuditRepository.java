package ar.com.anepanet.planificator.repository;

import ar.com.anepanet.planificator.domain.AuditEntry;
import ar.com.anepanet.planificator.domain.AuditUserOption;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Repository
public class AuditRepository {

    /** Días calendario del filtro en zona Argentina (no UTC del servidor). */
    private static final ZoneId FILTER_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final JdbcClient jdbc;

    public AuditRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(
            UUID userId,
            String username,
            String action,
            String entityType,
            String entityId,
            String summary) {
        jdbc.sql("""
                INSERT INTO audit_log (user_id, username, action, entity_type, entity_id, summary)
                VALUES (:userId, :username, :action, :entityType, :entityId, :summary)
                """)
                .param("userId", userId)
                .param("username", username)
                .param("action", action)
                .param("entityType", entityType)
                .param("entityId", entityId)
                .param("summary", summary)
                .update();
    }

    public List<AuditEntry> search(
            LocalDate from,
            LocalDate to,
            String username,
            String entityType,
            int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, occurred_at, user_id, username, action, entity_type, entity_id, summary
                FROM audit_log
                WHERE 1=1
                """);
        if (from != null) {
            sql.append(" AND occurred_at >= :fromTs");
        }
        if (to != null) {
            sql.append(" AND occurred_at < :toTs");
        }
        if (username != null && !username.isBlank()) {
            sql.append(" AND lower(username) = lower(:username)");
        }
        if (entityType != null && !entityType.isBlank()) {
            sql.append(" AND entity_type = :entityType");
        }
        sql.append(" ORDER BY occurred_at DESC LIMIT :limit");

        var stmt = jdbc.sql(sql.toString()).param("limit", limit);
        if (from != null) {
            stmt = stmt.param("fromTs", from.atStartOfDay(FILTER_ZONE).toOffsetDateTime());
        }
        if (to != null) {
            stmt = stmt.param("toTs", to.plusDays(1).atStartOfDay(FILTER_ZONE).toOffsetDateTime());
        }
        if (username != null && !username.isBlank()) {
            stmt = stmt.param("username", username.trim());
        }
        if (entityType != null && !entityType.isBlank()) {
            stmt = stmt.param("entityType", entityType.trim());
        }
        return stmt.query(this::map).list();
    }

    /** Usuarios de app + usernames históricos de la bitácora (p. ej. borrados). */
    public List<AuditUserOption> listFilterUsers() {
        return jdbc.sql("""
                SELECT username, display_name
                FROM (
                    SELECT u.username, u.display_name
                    FROM app_users u
                    UNION ALL
                    (
                        SELECT DISTINCT ON (lower(a.username)) a.username, a.username AS display_name
                        FROM audit_log a
                        WHERE NOT EXISTS (
                            SELECT 1 FROM app_users u WHERE lower(u.username) = lower(a.username)
                        )
                        ORDER BY lower(a.username), a.username
                    )
                ) t
                ORDER BY lower(username)
                """)
                .query((rs, rowNum) -> new AuditUserOption(
                        rs.getString("username"),
                        rs.getString("display_name")
                ))
                .list();
    }

    private AuditEntry map(ResultSet rs, int rowNum) throws SQLException {
        return new AuditEntry(
                rs.getObject("id", UUID.class),
                rs.getObject("occurred_at", OffsetDateTime.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("username"),
                rs.getString("action"),
                rs.getString("entity_type"),
                rs.getString("entity_id"),
                rs.getString("summary")
        );
    }
}
