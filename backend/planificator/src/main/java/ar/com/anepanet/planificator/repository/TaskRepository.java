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

    private static final String SELECT = """
            SELECT t.id, t.title, t.description, t.status, t.block_reason,
                   t.assignee_person_id, p.name AS assignee_name,
                   p.color AS assignee_color, t.on_board,
                   t.created_at, t.updated_at
            FROM tasks t
            LEFT JOIN people p ON p.id = t.assignee_person_id
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

    public Task insert(String title, String description) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO tasks (id, title, description)
                VALUES (:id, :title, :description)
                """)
                .param("id", id)
                .param("title", title)
                .param("description", description)
                .update();
        return findById(id).orElseThrow();
    }

    public Optional<Task> updateDetails(UUID id, String title, String description) {
        int changed = jdbc.sql("""
                UPDATE tasks
                SET title = :title, description = :description, updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("title", title)
                .param("description", description)
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
                    block_reason = NULL, assignee_person_id = NULL, updated_at = NOW()
                WHERE id = :id AND on_board = FALSE
                """)
                .param("id", id)
                .update();
        return changed == 0 ? Optional.empty() : findById(id);
    }

    public Optional<Task> assign(UUID id, UUID personId) {
        int changed = jdbc.sql("""
                UPDATE tasks
                SET assignee_person_id = :personId, updated_at = NOW()
                WHERE id = :id AND on_board = TRUE
                """)
                .param("id", id)
                .param("personId", personId)
                .update();
        return changed == 0 ? Optional.empty() : findById(id);
    }

    public Optional<Task> unassignToPending(UUID id) {
        int changed = jdbc.sql("""
                UPDATE tasks
                SET assignee_person_id = NULL, status = 'PENDING',
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
                SET on_board = FALSE, assignee_person_id = NULL, updated_at = NOW()
                WHERE id = :id AND on_board = TRUE
                """)
                .param("id", id)
                .update();
        return changed == 0 ? Optional.empty() : findById(id);
    }

    public boolean isOnVacation(UUID personId, LocalDate date) {
        Integer count = jdbc.sql("""
                SELECT COUNT(*)
                FROM shifts
                WHERE person_id = :personId
                  AND work_date = :workDate
                  AND location_id = 'vacaciones'
                """)
                .param("personId", personId)
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
                    from_assignee_person_id, to_assignee_person_id, block_reason
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
                .param("fromAssignee", before != null ? before.assigneePersonId() : null)
                .param("toAssignee", after != null ? after.assigneePersonId() : null)
                .param("blockReason", blockReason)
                .update();
    }

    private Task map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Task(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getString("block_reason"),
                rs.getObject("assignee_person_id", UUID.class),
                rs.getString("assignee_name"),
                rs.getString("assignee_color"),
                rs.getBoolean("on_board"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }
}
