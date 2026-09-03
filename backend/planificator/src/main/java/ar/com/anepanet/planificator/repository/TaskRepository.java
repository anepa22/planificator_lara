package ar.com.anepanet.planificator.repository;

import ar.com.anepanet.planificator.domain.Task;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TaskRepository {

    private static final String[] ASSIGNEE_COLORS = {
            "#0D9488", "#2563EB", "#C026D3", "#D97706", "#DC2626",
            "#059669", "#7C3AED", "#0891B2", "#DB2777", "#4F46E5"
    };

    private static final String SELECT = """
            SELECT t.id, t.title, t.description, t.status, t.block_reason,
                   t.location_id, l.name AS location_name, l.color AS location_color,
                   t.assignee_user_id,
                   COALESCE(NULLIF(u.display_name, ''), u.username) AS assignee_name,
                   t.on_board, t.created_at, t.updated_at
            FROM tasks t
            LEFT JOIN app_users u ON u.id = t.assignee_user_id
            LEFT JOIN locations l ON l.id = t.location_id
            """;

    private final JdbcClient jdbc;

    public TaskRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Task> findBoard() {
        return jdbc.sql(SELECT + """
                WHERE t.on_board = TRUE
                ORDER BY CASE t.status
                    WHEN 'PENDING' THEN 1
                    WHEN 'IN_PROGRESS' THEN 2
                    WHEN 'BLOCKED' THEN 3
                    WHEN 'DONE' THEN 4
                    WHEN 'VERIFIED' THEN 5
                    ELSE 6 END,
                    t.updated_at DESC
                """)
                .query(this::map)
                .list();
    }

    public List<Task> findAll() {
        return jdbc.sql(SELECT + " ORDER BY t.created_at DESC")
                .query(this::map)
                .list();
    }

    public Optional<Task> findById(UUID id) {
        return jdbc.sql(SELECT + " WHERE t.id = :id")
                .param("id", id)
                .query(this::map)
                .optional();
    }

    public Task insert(String title, String description, String locationId) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO tasks (id, title, description, location_id)
                VALUES (:id, :title, :description, :locationId)
                """)
                .param("id", id)
                .param("title", title)
                .param("description", description)
                .param("locationId", locationId)
                .update();
        return findById(id).orElseThrow();
    }

    public Optional<Task> updateDetails(UUID id, String title, String description, String locationId) {
        int changed = jdbc.sql("""
                UPDATE tasks
                SET title = :title, description = :description,
                    location_id = :locationId, updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("title", title)
                .param("description", description)
                .param("locationId", locationId)
                .update();
        return changed == 0 ? Optional.empty() : findById(id);
    }

    public boolean delete(UUID id) {
        return jdbc.sql("DELETE FROM tasks WHERE id = :id")
                .param("id", id)
                .update() > 0;
    }

    public Optional<Task> publish(UUID id) {
        int changed = jdbc.sql("""
                UPDATE tasks
                SET on_board = TRUE, status = 'PENDING',
                    block_reason = NULL, assignee_user_id = NULL, updated_at = NOW()
                WHERE id = :id AND on_board = FALSE
                """)
                .param("id", id)
                .update();
        return changed == 0 ? Optional.empty() : findById(id);
    }

    public Optional<Task> assign(UUID id, UUID userId) {
        int changed = jdbc.sql("""
                UPDATE tasks
                SET assignee_user_id = :userId, updated_at = NOW()
                WHERE id = :id AND on_board = TRUE
                """)
                .param("id", id)
                .param("userId", userId)
                .update();
        return changed == 0 ? Optional.empty() : findById(id);
    }

    public Optional<Task> unassignToPending(UUID id) {
        int changed = jdbc.sql("""
                UPDATE tasks
                SET assignee_user_id = NULL, status = 'PENDING',
                    block_reason = NULL, updated_at = NOW()
                WHERE id = :id AND on_board = TRUE
                """)
                .param("id", id)
                .update();
        return changed == 0 ? Optional.empty() : findById(id);
    }

    public Optional<Task> move(UUID id, String status, String blockReason) {
        int changed = jdbc.sql("""
                UPDATE tasks
                SET status = :status, block_reason = :blockReason, updated_at = NOW()
                WHERE id = :id AND on_board = TRUE
                """)
                .param("id", id)
                .param("status", status)
                .param("blockReason", blockReason)
                .update();
        return changed == 0 ? Optional.empty() : findById(id);
    }

    public Optional<Task> retire(UUID id) {
        int changed = jdbc.sql("""
                UPDATE tasks
                SET on_board = FALSE, assignee_user_id = NULL, updated_at = NOW()
                WHERE id = :id AND on_board = TRUE
                """)
                .param("id", id)
                .update();
        return changed == 0 ? Optional.empty() : findById(id);
    }

    public boolean isOnVacation(UUID userId, LocalDate date) {
        Integer count = jdbc.sql("""
                SELECT COUNT(*)
                FROM shifts
                WHERE user_id = :userId
                  AND work_date = :workDate
                  AND location_id = 'vacaciones'
                """)
                .param("userId", userId)
                .param("workDate", date)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    public void addHistory(
            Task before,
            Task after,
            UUID actorUserId,
            String action,
            String blockReason) {
        jdbc.sql("""
                INSERT INTO task_history (
                    task_id, actor_user_id, action, from_status, to_status,
                    from_assignee_user_id, to_assignee_user_id, block_reason
                ) VALUES (
                    :taskId, :actorUserId, :action, :fromStatus, :toStatus,
                    :fromAssignee, :toAssignee, :blockReason
                )
                """)
                .param("taskId", after != null ? after.id() : before.id())
                .param("actorUserId", actorUserId)
                .param("action", action)
                .param("fromStatus", before != null ? before.status() : null)
                .param("toStatus", after != null ? after.status() : null)
                .param("fromAssignee", before != null ? before.assigneeUserId() : null)
                .param("toAssignee", after != null ? after.assigneeUserId() : null)
                .param("blockReason", blockReason)
                .update();
    }

    private Task map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        UUID assigneeUserId = rs.getObject("assignee_user_id", UUID.class);
        return new Task(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getString("block_reason"),
                rs.getString("location_id"),
                rs.getString("location_name"),
                rs.getString("location_color"),
                assigneeUserId,
                rs.getString("assignee_name"),
                colorFor(assigneeUserId),
                rs.getBoolean("on_board"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    private static String colorFor(UUID id) {
        if (id == null) {
            return null;
        }
        return ASSIGNEE_COLORS[Math.floorMod(id.hashCode(), ASSIGNEE_COLORS.length)];
    }
}
