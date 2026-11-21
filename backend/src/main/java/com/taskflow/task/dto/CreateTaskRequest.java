package com.taskflow.task.dto;

import com.taskflow.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateTaskRequest(
        @NotNull UUID projectId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String description,
        TaskPriority priority,
        UUID assigneeId,
        Instant deadline
) {
}
