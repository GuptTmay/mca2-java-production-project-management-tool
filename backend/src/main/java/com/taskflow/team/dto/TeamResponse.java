package com.taskflow.team.dto;

import java.time.Instant;
import java.util.UUID;

public record TeamResponse(
        UUID id,
        String name,
        String description,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
