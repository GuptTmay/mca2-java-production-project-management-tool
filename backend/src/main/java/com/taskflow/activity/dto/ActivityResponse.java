package com.taskflow.activity.dto;

import com.taskflow.activity.ActivityType;
import com.taskflow.user.Role;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID taskId,
        UUID projectId,
        UUID actorId,
        String actorFirstName,
        String actorLastName,
        String actorEmail,
        ActivityType type,
        String description,
        String metadata,
        Instant createdAt
) {
}
