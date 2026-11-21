package com.taskflow.team.dto;

import com.taskflow.team.TeamRole;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String userFirstName,
        String userLastName,
        TeamRole teamRole,
        Instant joinedAt
) {
}
