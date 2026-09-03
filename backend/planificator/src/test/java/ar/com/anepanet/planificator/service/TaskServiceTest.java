package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.Person;
import ar.com.anepanet.planificator.domain.Task;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.PersonRepository;
import ar.com.anepanet.planificator.repository.TaskRepository;
import ar.com.anepanet.planificator.security.AuthUser;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.web.dto.AssignTaskRequest;
import ar.com.anepanet.planificator.web.dto.MoveTaskRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
    @Mock PersonRepository people;
    @Mock LocationRepository locations;
    @Mock AuthRepository auth;
    @Mock AuditService audit;

    private TaskService service;
    private UUID userId;
    private UUID personId;

    @BeforeEach
    void setUp() {
        service = new TaskService(tasks, people, locations, auth, audit);
        userId = UUID.randomUUID();
        personId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void personalCannotAssignTaskToAnotherPerson() {
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
    void assignmentIsRejectedWhenPersonIsOnVacation() {
        login(Permissions.TASKS_WRITE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(false)));
        UUID taskId = UUID.randomUUID();
        when(tasks.findById(taskId)).thenReturn(Optional.of(task(taskId, "PENDING", null)));
        when(people.findById(personId)).thenReturn(Optional.of(person()));
        when(tasks.isOnVacation(eq(personId), any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.assign(taskId, new AssignTaskRequest(personId)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(tasks, never()).assign(any(), any());
    }

    @Test
    void blockingRequiresReason() {
        login(Permissions.TASKS_WRITE);
        when(auth.findById(userId)).thenReturn(Optional.of(appUser(false)));
        UUID taskId = UUID.randomUUID();
        when(tasks.findById(taskId))
                .thenReturn(Optional.of(task(taskId, "IN_PROGRESS", personId)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.move(taskId, new MoveTaskRequest("BLOCKED", " ")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(tasks, never()).move(any(), any(), any());
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

    private void login(String... permissions) {
        List<SimpleGrantedAuthority> authorities =
                java.util.Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList();
        AuthUser principal = new AuthUser(userId, "test", authorities);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    private AppUser appUser(boolean manager) {
        List<String> permissions = manager
                ? List.of(Permissions.TASKS_WRITE, Permissions.TASKS_MANAGE)
                : List.of(Permissions.TASKS_WRITE);
        return new AppUser(
                userId, "test", "hash", "Test", personId, true,
                OffsetDateTime.now(), OffsetDateTime.now(), List.of("personal"), permissions);
    }

    private Person person() {
        return new Person(
                personId, "Persona", "#123456", true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    private Task task(UUID id, String status, UUID assignee) {
        return new Task(
                id, "Tarea", "Descripción", status, null,
                null, null, null, assignee,
                assignee == null ? null : "Persona", "#123456", true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }
}
