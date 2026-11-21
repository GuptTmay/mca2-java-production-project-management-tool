package com.taskflow.team.dto;

import com.taskflow.team.TeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record AddTeamMemberRequest(
        @NotBlank @Email String email,
        @NotNull TeamRole teamRole
) {
}
