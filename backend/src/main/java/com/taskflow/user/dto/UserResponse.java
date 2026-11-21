package com.taskflow.user.dto;

import com.taskflow.user.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
