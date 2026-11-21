package com.taskflow.project.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String userFirstName,
        String userLastName,
        Instant addedAt
) {
}
