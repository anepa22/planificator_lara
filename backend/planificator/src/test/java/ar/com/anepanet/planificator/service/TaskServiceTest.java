package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Task;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.TaskRepository;
import ar.com.anepanet.planificator.security.AuthUser;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.web.dto.AssignTaskRequest;
import ar.com.anepanet.planificator.web.dto.MoveTaskRequest;
import ar.com.anepanet.planificator.web.dto.TaskAssignee;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository tasks;
    @Mock LocationRepository locations;
    @Mock AuthRepository auth;
    @Mock AuditService audit;
    @Mock ApplicationEventPublisher events;

    private TaskService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new TaskService(tasks, locations, auth, audit, events);
        userId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void personalCannotAssignTaskToAnotherUser() {
        login(Permissions.TASKS_WRITE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(false)));
        UUID other = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(tasks.findById(taskId)).thenReturn(Optional.of(task(taskId, "PENDING", null)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.assign(taskId, new AssignTaskRequest(other)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(tasks, never()).assign(any(), any());
    }

    @Test
    void assigneesAreListedByPermissionRegardlessOfLoginAndRole() {
        login(Permissions.TASKS_WRITE, Permissions.TASKS_MANAGE);
        UUID withoutLogin = UUID.randomUUID();
        UUID supervisor = UUID.randomUUID();
        UUID withoutPermission = UUID.randomUUID();
        when(auth.findAllUsers()).thenReturn(List.of(
                user(withoutLogin, "brenda.cappa", "Brenda", false,
                        List.of("personal"), List.of(Permissions.TASKS_APPEAR)),
                user(supervisor, "gicemon", "Gisela", true,
                        List.of("editor"), List.of(Permissions.TASKS_APPEAR)),
                user(withoutPermission, "admin", "Administrador", true,
                        List.of("admin"), List.of(Permissions.TASKS_WRITE))));

        List<UUID> listed = service.assignees().stream().map(TaskAssignee::id).toList();

        assertEquals(List.of(withoutLogin, supervisor), listed);
    }

    @Test
    void assignmentIsRejectedWhenTargetLacksTaskPermission() {
        login(Permissions.TASKS_WRITE, Permissions.TASKS_MANAGE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(true)));
        UUID target = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(tasks.findById(taskId)).thenReturn(Optional.of(task(taskId, "PENDING", null)));
        when(auth.findById(target)).thenReturn(Optional.of(user(
                target, "solo.horarios", "Sin permiso", true,
                List.of("editor"), List.of(Permissions.SHIFTS_WRITE))));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.assign(taskId, new AssignTaskRequest(target)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(tasks, never()).assign(any(), any());
    }

    @Test
    void assignmentIsRejectedWhenUserIsOnVacation() {
        login(Permissions.TASKS_WRITE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(false)));
        UUID taskId = UUID.randomUUID();
        when(tasks.findById(taskId)).thenReturn(Optional.of(task(taskId, "PENDING", null)));
        when(tasks.isOnVacation(eq(userId), any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.assign(taskId, new AssignTaskRequest(userId)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(tasks, never()).assign(any(), any());
    }

    @Test
    void blockingRequiresReason() {
        login(Permissions.TASKS_WRITE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(false)));
        UUID taskId = UUID.randomUUID();
        when(tasks.findById(taskId))
                .thenReturn(Optional.of(task(taskId, "IN_PROGRESS", userId)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.move(taskId, new MoveTaskRequest("BLOCKED", " ")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(tasks, never()).move(any(), any(), any());
    }

    @Test
    void personalCanDragOwnAssignedTaskFromPending() {
        login(Permissions.TASKS_WRITE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(false)));
        UUID taskId = UUID.randomUUID();
        Task before = task(taskId, "PENDING", userId);
        Task after = task(taskId, "IN_PROGRESS", userId);
        when(tasks.findById(taskId)).thenReturn(Optional.of(before));
        when(tasks.move(taskId, "IN_PROGRESS", null)).thenReturn(Optional.of(after));

        Task result = service.move(taskId, new MoveTaskRequest("IN_PROGRESS", null));

        assertEquals("IN_PROGRESS", result.status());
        assertEquals(userId, result.assigneeUserId());
        verify(tasks).move(taskId, "IN_PROGRESS", null);
    }

    @Test
    void movingBackToPendingKeepsAssignee() {
        login(Permissions.TASKS_WRITE, Permissions.TASKS_MANAGE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(true)));
        UUID taskId = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        Task before = task(taskId, "IN_PROGRESS", assignee);
        Task after = task(taskId, "PENDING", assignee);
        when(tasks.findById(taskId)).thenReturn(Optional.of(before));
        when(tasks.move(taskId, "PENDING", null)).thenReturn(Optional.of(after));

        Task result = service.move(taskId, new MoveTaskRequest("PENDING", null));

        assertEquals("PENDING", result.status());
        assertEquals(assignee, result.assigneeUserId());
        verify(tasks).move(taskId, "PENDING", null);
        verify(tasks, never()).unassignToPending(any());
    }

    @Test
    void managerCanVerifyAnyAssignedTask() {
        login(Permissions.TASKS_WRITE, Permissions.TASKS_MANAGE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(true)));
        UUID taskId = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Task before = task(taskId, "DONE", other);
        Task after = task(taskId, "VERIFIED", other);
        when(tasks.findById(taskId)).thenReturn(Optional.of(before));
        when(tasks.move(taskId, "VERIFIED", null)).thenReturn(Optional.of(after));

        Task result = service.move(taskId, new MoveTaskRequest("VERIFIED", null));

        assertEquals("VERIFIED", result.status());
        verify(tasks).addHistory(before, after, userId, "MOVE", null);
    }

    @Test
    void statusChangePublishesTelegramEventForSubscribedUsers() {
        login(Permissions.TASKS_WRITE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(false)));
        when(auth.findTelegramChatIdsWithPermission(Permissions.TASKS_NOTIFY))
                .thenReturn(List.of("123456789"));
        UUID taskId = UUID.randomUUID();
        Task before = task(taskId, "PENDING", userId);
        Task after = task(taskId, "IN_PROGRESS", userId);
        when(tasks.findById(taskId)).thenReturn(Optional.of(before));
        when(tasks.move(taskId, "IN_PROGRESS", null)).thenReturn(Optional.of(after));

        service.move(taskId, new MoveTaskRequest("IN_PROGRESS", null));

        verify(events).publishEvent(any(TaskChangedEvent.class));
    }

    @Test
    void assignmentPublishesTelegramEventEvenWithoutStatusChange() {
        login(Permissions.TASKS_WRITE, Permissions.TASKS_MANAGE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(true)));
        when(auth.findTelegramChatIdsWithPermission(Permissions.TASKS_NOTIFY))
                .thenReturn(List.of("123456789"));
        UUID taskId = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        Task before = task(taskId, "PENDING", null);
        Task after = task(taskId, "PENDING", assignee);
        when(tasks.findById(taskId)).thenReturn(Optional.of(before));
        when(auth.findById(assignee)).thenReturn(Optional.of(staffUser(assignee)));
        when(tasks.assign(taskId, assignee)).thenReturn(Optional.of(after));

        service.assign(taskId, new AssignTaskRequest(assignee));

        verify(events).publishEvent(any(TaskChangedEvent.class));
    }

    private void login(String... permissions) {
        List<SimpleGrantedAuthority> authorities =
                java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        AuthUser principal = new AuthUser(userId, "test", authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private AppUser appUser(boolean manager) {
        return appUser(manager, null);
    }

    private AppUser staffUser(UUID id) {
        return user(id, "asistente", "Brenda", true,
                List.of("personal"), List.of(Permissions.TASKS_WRITE, Permissions.TASKS_APPEAR));
    }

    private AppUser user(
            UUID id,
            String username,
            String displayName,
            boolean canLogin,
            List<String> roleIds,
            List<String> permissions) {
        return new AppUser(
                id, username, "hash", displayName, "#123456", null, true, canLogin, false,
                OffsetDateTime.now(), OffsetDateTime.now(), roleIds, permissions);
    }

    private AppUser appUser(boolean manager, String telegramChatId) {
        List<String> permissions = manager
                ? List.of(Permissions.TASKS_WRITE, Permissions.TASKS_MANAGE)
                : List.of(Permissions.TASKS_WRITE, Permissions.TASKS_APPEAR);
        return new AppUser(
                userId, "test", "hash", "Test", "#123456", telegramChatId, true, true, false,
                OffsetDateTime.now(), OffsetDateTime.now(), List.of("personal"), permissions);
    }

    private Task task(UUID id, String status, UUID assignee) {
        return new Task(
                id, "Tarea", "Descripción", status, null,
                null, null, null, assignee,
                assignee == null ? null : "Test", "#123456", true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }
}
