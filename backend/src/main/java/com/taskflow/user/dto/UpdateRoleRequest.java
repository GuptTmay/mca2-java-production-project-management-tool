package com.taskflow.user.dto;

import com.taskflow.user.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull Role role
) {
}
