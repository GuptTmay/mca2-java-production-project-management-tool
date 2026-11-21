package com.taskflow.task.dto;

import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeId,
        UUID reporterId,
        Instant deadline,
        Set<LabelResponse> labels,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
