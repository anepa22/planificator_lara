package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.domain.Task;
import ar.com.anepanet.planificator.service.TaskService;
import ar.com.anepanet.planificator.service.TaskRetentionService;
import ar.com.anepanet.planificator.web.dto.AssignTaskRequest;
import ar.com.anepanet.planificator.web.dto.CreateTaskRequest;
import ar.com.anepanet.planificator.web.dto.MoveTaskRequest;
import ar.com.anepanet.planificator.web.dto.TaskAssignee;
import ar.com.anepanet.planificator.web.dto.TaskHistoryResponse;
import ar.com.anepanet.planificator.web.dto.TaskRetentionSettings;
import ar.com.anepanet.planificator.web.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService tasks;
    private final TaskRetentionService retention;

    public TaskController(TaskService tasks, TaskRetentionService retention) {
        this.tasks = tasks;
        this.retention = retention;
    }

    @GetMapping("/board")
    public List<Task> board() {
        return tasks.board();
    }

    @GetMapping("/{id}/history")
    public TaskHistoryResponse history(@PathVariable UUID id) {
        return tasks.history(id);
    }

    @GetMapping("/settings/retention")
    public TaskRetentionSettings retentionSettings() {
        return retention.settings();
    }

    @PutMapping("/settings/retention")
    public TaskRetentionSettings updateRetentionSettings(
            @Valid @RequestBody TaskRetentionSettings request) {
        return retention.update(request);
    }

    @GetMapping("/assignees")
    public List<TaskAssignee> assignees() {
        return tasks.assignees();
    }

    @GetMapping
    public List<Task> listAll() {
        return tasks.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@Valid @RequestBody CreateTaskRequest request) {
        return tasks.create(request);
    }

    @PutMapping("/{id}")
    public Task update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return tasks.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tasks.delete(id);
    }

    @PostMapping("/{id}/publish")
    public Task publish(@PathVariable UUID id) {
        return tasks.publish(id);
    }

    @PostMapping("/{id}/assign")
    public Task assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTaskRequest request) {
        return tasks.assign(id, request);
    }

    @PostMapping("/{id}/unassign")
    public Task unassign(@PathVariable UUID id) {
        return tasks.unassign(id);
    }

    @PostMapping("/{id}/move")
    public Task move(
            @PathVariable UUID id,
            @Valid @RequestBody MoveTaskRequest request) {
        return tasks.move(id, request);
    }

    @PostMapping("/{id}/retire")
    public Task retire(@PathVariable UUID id) {
        return tasks.retire(id);
    }
}
