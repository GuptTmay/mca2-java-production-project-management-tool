package com.taskflow.task;

import java.time.Instant;
import java.util.UUID;

public record TaskFilter(
        UUID projectId,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeId,
        Instant deadlineFrom,
        Instant deadlineTo,
        String search
) {
}
