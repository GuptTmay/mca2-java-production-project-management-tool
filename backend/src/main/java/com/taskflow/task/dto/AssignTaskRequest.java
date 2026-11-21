package com.taskflow.task.dto;

import java.util.UUID;

/**
 * assigneeId is intentionally not @NotNull - passing null unassigns the task.
 */
public record AssignTaskRequest(
        UUID assigneeId
) {
}
