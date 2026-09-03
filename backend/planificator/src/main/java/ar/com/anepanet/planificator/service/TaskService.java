package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.Person;
import ar.com.anepanet.planificator.domain.Task;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.PersonRepository;
import ar.com.anepanet.planificator.repository.TaskRepository;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.security.SecurityUtils;
import ar.com.anepanet.planificator.web.dto.AssignTaskRequest;
import ar.com.anepanet.planificator.web.dto.CreateTaskRequest;
import ar.com.anepanet.planificator.web.dto.MoveTaskRequest;
import ar.com.anepanet.planificator.web.dto.UpdateTaskRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
    private final PersonRepository people;
    private final LocationRepository locations;
    private final AuthRepository auth;
    private final AuditService audit;

    public TaskService(
            TaskRepository tasks,
            PersonRepository people,
            LocationRepository locations,
            AuthRepository auth,
            AuditService audit) {
        this.tasks = tasks;
        this.people = people;
        this.locations = locations;
        this.auth = auth;
        this.audit = audit;
    }

    public List<Task> board() {
        return tasks.findBoard();
    }

    public List<Task> listAll() {
        requireManager();
        return tasks.findAll();
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
            if (user.personId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "El usuario no está vinculado a una persona");
            }
            if (!user.personId().equals(req.personId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Solo puede asignarse tareas a sí mismo");
            }
            if (!"PENDING".equals(before.status()) || before.assigneePersonId() != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "La tarea ya fue asignada o no está Pendiente");
            }
        }

        Person person = people.findById(req.personId())
                .filter(Person::active)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Persona inválida o inactiva"));
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (tasks.isOnVacation(person.id(), today)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    person.name() + " está de vacaciones y no se le puede asignar la tarea");
        }

        Task updated = tasks.assign(id, person.id()).orElseThrow(() -> notFound());
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
        if (before.assigneePersonId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Hay que asignar la tarea antes de moverla");
        }

        if (!isManager()) {
            if (user.personId() == null || !user.personId().equals(before.assigneePersonId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Solo puede mover sus propias tareas");
            }
            if (!PERSONAL_DESTINATIONS.contains(destination)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Personal solo puede mover a En proceso, Bloqueada o Terminada");
            }
        }

        String blockReason = cleanDescription(req.blockReason());
        if ("BLOCKED".equals(destination) && (blockReason == null || blockReason.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El motivo de bloqueo es obligatorio");
        }

        Task updated;
        if ("PENDING".equals(destination)) {
            requireManager();
            updated = tasks.unassignToPending(id).orElseThrow(() -> notFound());
        } else {
            updated = tasks.move(id, destination, blockReason).orElseThrow(() -> notFound());
        }
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
        tasks.addHistory(before, after, currentUser().id(), action, blockReason);
        audit.record(AuditService.ACTION_UPDATE, AuditService.TYPE_TASK,
                after.id().toString(), after.title() + " · " + action);
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
