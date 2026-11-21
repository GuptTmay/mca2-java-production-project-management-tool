package com.taskflow.task.dto;

import com.taskflow.task.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskHistoryResponse(
        UUID id,
        UUID taskId,
        UUID projectId,
        UUID actorId,
        String actorName,
        TaskStatus previousStatus,
        TaskStatus newStatus,
        String changeReason,
        Instant createdAt
) {
}
