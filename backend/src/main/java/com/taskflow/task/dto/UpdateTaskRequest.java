package com.taskflow.task.dto;

import com.taskflow.task.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10000) String description,
        @NotNull TaskPriority priority,
        Instant deadline
) {
}
