package com.taskflow.task;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.task.dto.AssignTaskRequest;
import com.taskflow.task.dto.CreateTaskRequest;
import com.taskflow.task.dto.TaskResponse;
import com.taskflow.task.dto.UpdateTaskRequest;
import com.taskflow.task.dto.UpdateTaskStatusRequest;
import com.taskflow.user.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request, @AuthenticationPrincipal User currentUser) {
        return taskService.createTask(request, currentUser);
    }

    @GetMapping
    public PageResponse<TaskResponse> list(
            @RequestParam UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) Instant deadlineFrom,
            @RequestParam(required = false) Instant deadlineTo,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        TaskFilter filter = new TaskFilter(projectId, status, priority, assigneeId, deadlineFrom, deadlineTo, search);
        return taskService.listTasks(filter, currentUser, pageable);
    }

    @GetMapping("/board")
    public PageResponse<TaskResponse> board(
            @RequestParam UUID projectId,
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 200) Pageable pageable) {
        return taskService.getBoard(projectId, currentUser, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return taskService.getTask(id, currentUser);
    }

    @PatchMapping("/{id}")
    public TaskResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal User currentUser) {
        return taskService.updateTask(id, request, currentUser);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            @AuthenticationPrincipal User currentUser) {
        return taskService.updateStatus(id, request, currentUser);
    }

    @PatchMapping("/{id}/assign")
    public TaskResponse assign(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTaskRequest request,
            @AuthenticationPrincipal User currentUser) {
        return taskService.assignTask(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        taskService.deleteTask(id, currentUser);
    }

    @PostMapping("/{id}/labels/{labelId}")
    public TaskResponse addLabel(
            @PathVariable UUID id, @PathVariable UUID labelId, @AuthenticationPrincipal User currentUser) {
        return taskService.addLabel(id, labelId, currentUser);
    }

    @DeleteMapping("/{id}/labels/{labelId}")
    public TaskResponse removeLabel(
            @PathVariable UUID id, @PathVariable UUID labelId, @AuthenticationPrincipal User currentUser) {
        return taskService.removeLabel(id, labelId, currentUser);
    }
}
