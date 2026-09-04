package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.Task;
import ar.com.anepanet.planificator.domain.TaskHistory;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.TaskRepository;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.security.SecurityUtils;
import ar.com.anepanet.planificator.web.dto.AssignTaskRequest;
import ar.com.anepanet.planificator.web.dto.CreateTaskRequest;
import ar.com.anepanet.planificator.web.dto.MoveTaskRequest;
import ar.com.anepanet.planificator.web.dto.TaskAssignee;
import ar.com.anepanet.planificator.web.dto.TaskHistoryResponse;
import ar.com.anepanet.planificator.web.dto.UpdateTaskRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskService {

    private static final Set<String> STATUSES =
            Set.of("PENDING", "IN_PROGRESS", "BLOCKED", "DONE", "VERIFIED");
    private static final Set<String> PERSONAL_DESTINATIONS =
            Set.of("IN_PROGRESS", "BLOCKED", "DONE");
    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("America/Argentina/Buenos_Aires");

    private static final String PERSONAL_ROLE = "personal";

    private final TaskRepository tasks;
    private final LocationRepository locations;
    private final AuthRepository auth;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public TaskService(
            TaskRepository tasks,
            LocationRepository locations,
            AuthRepository auth,
            AuditService audit,
            ApplicationEventPublisher events) {
        this.tasks = tasks;
        this.locations = locations;
        this.auth = auth;
        this.audit = audit;
        this.events = events;
    }

    public List<Task> board() {
        return tasks.findBoard();
    }

    public TaskHistoryResponse history(UUID id) {
        find(id);
        List<TaskHistory> all = tasks.findHistory(id);
        List<TaskHistory> moves = all.stream().filter(TaskService::isMovement).toList();
        List<TaskHistory> visible = SecurityUtils.hasAuthority(Permissions.TASKS_HISTORY)
                ? moves
                : List.of();
        return new TaskHistoryResponse(
                lastMatching(all, TaskService::wentPending),
                lastMatching(all, TaskService::isAssign),
                lastMatching(all, h -> enteredStatus(h, "BLOCKED")),
                lastMatching(all, h -> enteredStatus(h, "DONE")),
                lastMatching(all, h -> enteredStatus(h, "VERIFIED")),
                visible
        );
    }

    public List<Task> listAll() {
        requireManager();
        return tasks.findAll();
    }

    public List<TaskAssignee> assignees() {
        requireManager();
        return auth.findAllUsers().stream()
                .filter(AppUser::active)
                .filter(AppUser::canLogin)
                .filter(user -> user.roleIds() != null && user.roleIds().contains(PERSONAL_ROLE))
                .map(user -> new TaskAssignee(
                        user.id(),
                        user.username(),
                        displayName(user)))
                .toList();
    }

    @Transactional
    public Task create(CreateTaskRequest req) {
        requireManager();
        String locationId = resolveLocationId(req.locationId());
        Task created = tasks.insert(
                cleanTitle(req.title()), cleanDescription(req.description()), locationId);
        tasks.addHistory(null, created, currentUser().id(), "CREATE", null);
        audit.record(AuditService.ACTION_CREATE, AuditService.TYPE_TASK,
                created.id().toString(), created.title());
        return created;
    }

    @Transactional
    public Task update(UUID id, UpdateTaskRequest req) {
        requireManager();
        Task before = find(id);
        Task updated = tasks.updateDetails(
                        id,
                        cleanTitle(req.title()),
                        cleanDescription(req.description()),
                        resolveLocationId(req.locationId()))
                .orElseThrow(() -> notFound());
        tasks.addHistory(before, updated, currentUser().id(), "UPDATE", null);
        audit.record(AuditService.ACTION_UPDATE, AuditService.TYPE_TASK,
                updated.id().toString(), updated.title());
        return updated;
    }

    @Transactional
    public void delete(UUID id) {
        requireManager();
        Task task = find(id);
        if (!tasks.delete(id)) {
            throw notFound();
        }
        audit.record(AuditService.ACTION_DELETE, AuditService.TYPE_TASK,
                id.toString(), task.title());
    }

    @Transactional
    public Task publish(UUID id) {
        requireManager();
        Task before = find(id);
        if (before.onBoard()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La tarea ya está en el tablero");
        }
        Task updated = tasks.publish(id).orElseThrow(() -> notFound());
        recordChange(before, updated, "PUBLISH", null);
        return updated;
    }

    @Transactional
    public Task assign(UUID id, AssignTaskRequest req) {
        SecurityUtils.requireAuthority(Permissions.TASKS_WRITE);
        AppUser user = currentUser();
        Task before = find(id);
        if (!before.onBoard()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La tarea no está en el tablero");
        }

        boolean manager = isManager();
        if (!manager) {
            if (!user.id().equals(req.userId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Solo puede asignarse tareas a sí mismo");
            }
            if (!"PENDING".equals(before.status()) || before.assigneeUserId() != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "La tarea ya fue asignada o no está Pendiente");
            }
        }

        AppUser target = auth.findById(req.userId())
                .filter(AppUser::active)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Usuario inválido o inactivo"));
        if (target.roleIds() == null || !target.roleIds().contains(PERSONAL_ROLE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Solo se puede asignar a usuarios con rol Asistente");
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (tasks.isOnVacation(target.id(), today)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    displayName(target) + " está de vacaciones y no se le puede asignar la tarea");
        }

        Task updated = tasks.assign(id, target.id()).orElseThrow(() -> notFound());
        recordChange(before, updated, "ASSIGN", null);
        return updated;
    }

    @Transactional
    public Task unassign(UUID id) {
        requireManager();
        Task before = find(id);
        if (!before.onBoard()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La tarea no está en el tablero");
        }
        Task updated = tasks.unassignToPending(id).orElseThrow(() -> notFound());
        recordChange(before, updated, "UNASSIGN", null);
        return updated;
    }

    @Transactional
    public Task move(UUID id, MoveTaskRequest req) {
        SecurityUtils.requireAuthority(Permissions.TASKS_WRITE);
        AppUser user = currentUser();
        Task before = find(id);
        String destination = normalizeStatus(req.status());

        if (!before.onBoard()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La tarea no está en el tablero");
        }
        if (before.assigneeUserId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Hay que asignar la tarea antes de moverla");
        }

        if (!isManager()) {
            if (!user.id().equals(before.assigneeUserId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Solo puede mover sus propias tareas");
            }
            if (!PERSONAL_DESTINATIONS.contains(destination)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Un asistente solo puede mover a En proceso, Bloqueada o Terminada");
            }
        }

        String blockReason = cleanDescription(req.blockReason());
        if ("BLOCKED".equals(destination) && (blockReason == null || blockReason.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El motivo de bloqueo es obligatorio");
        }

        if ("PENDING".equals(destination)) {
            requireManager();
        }
        Task updated = tasks.move(id, destination, blockReason).orElseThrow(() -> notFound());
        recordChange(before, updated, "MOVE", blockReason);
        return updated;
    }

    @Transactional
    public Task retire(UUID id) {
        requireManager();
        Task before = find(id);
        if (!before.onBoard()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La tarea no está en el tablero");
        }
        Task updated = tasks.retire(id).orElseThrow(() -> notFound());
        recordChange(before, updated, "RETIRE", null);
        return updated;
    }

    private void recordChange(Task before, Task after, String action, String blockReason) {
        AppUser actor = currentUser();
        tasks.addHistory(before, after, actor.id(), action, blockReason);
        audit.record(AuditService.ACTION_UPDATE, AuditService.TYPE_TASK,
                after.id().toString(), after.title() + " · " + action);
        publishStatusChange(before, after, blockReason, actor);
    }

    private void publishStatusChange(
            Task before,
            Task after,
            String blockReason,
            AppUser actor) {
        if (Objects.equals(before.status(), after.status())) {
            return;
        }
        UUID recipientId = after.assigneeUserId() != null
                ? after.assigneeUserId()
                : before.assigneeUserId();
        if (recipientId == null) {
            return;
        }
        auth.findById(recipientId)
                .filter(AppUser::active)
                .filter(user -> user.telegramChatId() != null
                        && !user.telegramChatId().isBlank())
                .ifPresent(recipient -> events.publishEvent(new TaskStatusChangedEvent(
                        recipient.telegramChatId(),
                        after.title(),
                        before.status(),
                        after.status(),
                        after.assigneeName() != null
                                ? after.assigneeName()
                                : before.assigneeName(),
                        after.locationName(),
                        blockReason,
                        displayName(actor)
                )));
    }

    private static TaskHistory lastMatching(
            List<TaskHistory> all,
            java.util.function.Predicate<TaskHistory> match) {
        for (int i = all.size() - 1; i >= 0; i--) {
            TaskHistory entry = all.get(i);
            if (match.test(entry)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean isMovement(TaskHistory entry) {
        String action = entry.action();
        return "MOVE".equals(action)
                || "PUBLISH".equals(action)
                || "UNASSIGN".equals(action)
                || "RETIRE".equals(action)
                || "RETIRE_AUTO".equals(action);
    }

    private static boolean wentPending(TaskHistory entry) {
        return enteredStatus(entry, "PENDING") || "PUBLISH".equals(entry.action())
                || "UNASSIGN".equals(entry.action());
    }

    private static boolean isAssign(TaskHistory entry) {
        return "ASSIGN".equals(entry.action()) && entry.toAssigneeUserId() != null;
    }

    private static boolean enteredStatus(TaskHistory entry, String status) {
        if (!status.equals(entry.toStatus())) {
            return false;
        }
        if (status.equals(entry.fromStatus()) && !"PUBLISH".equals(entry.action())) {
            return false;
        }
        return "MOVE".equals(entry.action())
                || "PUBLISH".equals(entry.action())
                || "UNASSIGN".equals(entry.action());
    }

    private Task find(UUID id) {
        return tasks.findById(id).orElseThrow(() -> notFound());
    }

    private AppUser currentUser() {
        UUID id = SecurityUtils.currentUser()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "No autenticado"))
                .getId();
        return auth.findById(id)
                .filter(AppUser::active)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario no válido"));
    }

    private boolean isManager() {
        return SecurityUtils.hasAuthority(Permissions.TASKS_MANAGE);
    }

    private void requireManager() {
        SecurityUtils.requireAuthority(Permissions.TASKS_MANAGE);
    }

    private static String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de tarea inválido");
        }
        return normalized;
    }

    private static String displayName(AppUser user) {
        if (user.displayName() == null || user.displayName().isBlank()) {
            return user.username();
        }
        return user.displayName();
    }

    private String resolveLocationId(String locationId) {
        if (locationId == null || locationId.isBlank()) {
            return null;
        }
        String id = locationId.trim();
        Location location = locations.findById(id)
                .filter(Location::active)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Local inválido o inactivo"));
        if (Permissions.isAbsenceLocation(location.id())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La tarea no puede asociarse a vacaciones o franco");
        }
        return location.id();
    }

    private static String cleanTitle(String title) {
        return title == null ? "" : title.trim();
    }

    private static String cleanDescription(String description) {
        if (description == null) {
            return null;
        }
        String cleaned = description.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
    }
}
