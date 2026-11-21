package com.taskflow.task;

import com.taskflow.task.dto.TaskHistoryResponse;
import com.taskflow.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/history")
public class TaskHistoryController {

    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskHistoryMapper taskHistoryMapper;
    private final TaskService taskService;

    public TaskHistoryController(TaskHistoryRepository taskHistoryRepository, TaskHistoryMapper taskHistoryMapper, TaskService taskService) {
        this.taskHistoryRepository = taskHistoryRepository;
        this.taskHistoryMapper = taskHistoryMapper;
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskHistoryResponse> list(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 50) Pageable pageable) {
        taskService.getTask(taskId, currentUser);
        return taskHistoryRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(taskHistoryMapper::toResponse)
                .toList();
    }
}
