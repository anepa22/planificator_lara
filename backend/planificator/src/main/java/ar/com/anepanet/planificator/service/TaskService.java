package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.Task;
import ar.com.anepanet.planificator.domain.TaskHistory;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.TaskRepository;
import ar.com.anepanet.planificator.realtime.PlanificatorChangedEvent;
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
        Task task = find(id);
        boolean fullHistory = SecurityUtils.hasAuthority(Permissions.TASKS_HISTORY)
                || (isStockAdmin() && task.stock());
        if (!fullHistory && isStockAdmin()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No puede ver el detalle de esta tarea");
        }
        List<TaskHistory> all = tasks.findHistory(id);
        List<TaskHistory> moves = all.stream().filter(TaskService::isMovement).toList();
        List<TaskHistory> visible = fullHistory ? moves : List.of();
        String status = task.status();
        return new TaskHistoryResponse(
                lastMatching(all, TaskService::wentPending),
                lastMatching(all, TaskService::isAssign),
                "BLOCKED".equals(status)
                        ? lastMatching(all, h -> enteredStatus(h, "BLOCKED"))
                        : null,
                showsDoneMilestone(status)
                        ? lastMatching(all, h -> enteredStatus(h, "DONE"))
                        : null,
                "VERIFIED".equals(status)
                        ? lastMatching(all, h -> enteredStatus(h, "VERIFIED"))
                        : null,
                visible
        );
    }

    public List<Task> listAll() {
        requireCatalogAccess();
        List<Task> all = tasks.findAll();
        if (isManager()) {
            return all;
        }
        return all.stream().filter(Task::stock).toList();
    }

    public List<TaskAssignee> assignees() {
        requireCatalogAccess();
        return auth.findAllUsers().stream()
                .filter(AppUser::active)
                .filter(TaskService::canAppearOnTasks)
                .map(user -> new TaskAssignee(
                        user.id(),
                        user.username(),
                        displayName(user)))
                .toList();
    }

    @Transactional
    public Task create(CreateTaskRequest req) {
        requireCatalogAccess();
        String locationId = resolveLocationId(req.locationId());
        boolean stock = !isManager() || Boolean.TRUE.equals(req.stock());
        Task created = tasks.insert(
                cleanTitle(req.title()), cleanDescription(req.description()), locationId, stock);
        tasks.addHistory(null, created, currentUser().id(), "CREATE", null);
        audit.record(AuditService.ACTION_CREATE, AuditService.TYPE_TASK,
                created.id().toString(), created.title());
        publishRealtimeTaskChange();
        return created;
    }

    @Transactional
    public Task update(UUID id, UpdateTaskRequest req) {
        Task before = find(id);
        requireTaskManager(before);
        boolean stock = nextStock(before, req.stock());
        Task updated = tasks.updateDetails(
                        id,
                        cleanTitle(req.title()),
                        cleanDescription(req.description()),
                        resolveLocationId(req.locationId()),
                        stock)
                .orElseThrow(() -> notFound());
        tasks.addHistory(before, updated, currentUser().id(), "UPDATE", null);
        audit.record(AuditService.ACTION_UPDATE, AuditService.TYPE_TASK,
                updated.id().toString(), updated.title());
        publishRealtimeTaskChange();
        return updated;
    }

    @Transactional
    public void delete(UUID id) {
        Task task = find(id);
        requireTaskManager(task);
        if (!tasks.delete(id)) {
            throw notFound();
        }
        audit.record(AuditService.ACTION_DELETE, AuditService.TYPE_TASK,
                id.toString(), task.title());
        publishRealtimeTaskChange();
    }

    @Transactional
    public Task publish(UUID id) {
        Task before = find(id);
        requireTaskManager(before);
        if (before.onBoard()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La tarea ya está en el tablero");
        }
        Task updated = tasks.publish(id).orElseThrow(() -> notFound());
        recordChange(before, updated, "PUBLISH", null);
        return updated;
    }

    @Transactional
    public Task assign(UUID id, AssignTaskRequest req) {
        AppUser user = currentUser();
        Task before = find(id);
        if (!before.onBoard()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La tarea no está en el tablero");
        }

        boolean manager = canManage(before);
        if (!manager) {
            if (isStockAdmin()) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No puede administrar esta tarea");
            }
            SecurityUtils.requireAuthority(Permissions.TASKS_WRITE);
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
        if (!canAppearOnTasks(target)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    displayName(target) + " no tiene el permiso para aparecer en tareas");
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
        Task before = find(id);
        requireTaskManager(before);
        if (!before.onBoard()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La tarea no está en el tablero");
        }
        Task updated = tasks.unassignToPending(id).orElseThrow(() -> notFound());
        recordChange(before, updated, "UNASSIGN", null);
        return updated;
    }

    @Transactional
    public Task move(UUID id, MoveTaskRequest req) {
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

        boolean manager = canManage(before);
        if (!manager) {
            if (isStockAdmin()) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No puede administrar esta tarea");
            }
            SecurityUtils.requireAuthority(Permissions.TASKS_WRITE);
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

        if ("PENDING".equals(destination) && !manager) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No puede devolver la tarea a Pendientes");
        }
        Task updated = tasks.move(id, destination, blockReason).orElseThrow(() -> notFound());
        recordChange(before, updated, "MOVE", blockReason);
        return updated;
    }

    @Transactional
    public Task retire(UUID id) {
        Task before = find(id);
        requireTaskManager(before);
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
        publishRealtimeTaskChange();
        publishChange(before, after, blockReason, actor);
    }

    private void publishRealtimeTaskChange() {
        events.publishEvent(new PlanificatorChangedEvent(PlanificatorChangedEvent.TASKS));
    }

    private void publishChange(
            Task before,
            Task after,
            String blockReason,
            AppUser actor) {
        boolean statusChanged = !Objects.equals(before.status(), after.status());
        boolean assigneeChanged =
                !Objects.equals(before.assigneeUserId(), after.assigneeUserId());
        if (!statusChanged && !assigneeChanged) {
            return;
        }
        List<String> chatIds = auth.findTelegramChatIdsWithPermission(Permissions.TASKS_NOTIFY);
        if (chatIds.isEmpty()) {
            return;
        }
        events.publishEvent(new TaskChangedEvent(
                chatIds,
                after.title(),
                before.status(),
                after.status(),
                after.assigneeName(),
                before.assigneeName(),
                assigneeChanged,
                after.locationName(),
                blockReason,
                displayName(actor)
        ));
    }

    private static boolean showsDoneMilestone(String status) {
        return "DONE".equals(status) || "VERIFIED".equals(status);
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

    private boolean isStockAdmin() {
        return SecurityUtils.hasAuthority(Permissions.TASKS_STOCK);
    }

    private boolean canManage(Task task) {
        return isManager() || (isStockAdmin() && task.stock());
    }

    private void requireCatalogAccess() {
        if (!isManager() && !isStockAdmin()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No puede administrar tareas");
        }
    }

    private void requireTaskManager(Task task) {
        if (!canManage(task)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No puede administrar esta tarea");
        }
    }

    private boolean nextStock(Task before, Boolean requested) {
        if (!isManager()) {
            if (Boolean.FALSE.equals(requested)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No puede quitar la marca de stock");
            }
            return true;
        }
        return requested == null ? before.stock() : requested;
    }

    private static boolean canAppearOnTasks(AppUser user) {
        return user.permissions() != null
                && user.permissions().contains(Permissions.TASKS_APPEAR);
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
