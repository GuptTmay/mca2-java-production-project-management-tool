package com.taskflow.team;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralizes team-scoped resource-level authorization so that any service
 * touching team-owned resources (teams themselves, projects, and eventually
 * anything else scoped to a team) enforces the exact same rules.
 */
@Component
public class TeamAccessGuard {

    private final TeamMemberRepository teamMemberRepository;

    public TeamAccessGuard(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    /** Any team member, or a global ADMIN. */
    public void requireMembership(UUID teamId, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUser.getId())) {
            throw new ForbiddenOperationException("You are not a member of this team");
        }
    }

    /** Team OWNER or MANAGER, or a global ADMIN. */
    public void requireManageAccess(UUID teamId, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenOperationException("You are not a member of this team"));
        if (membership.getTeamRole() != TeamRole.OWNER && membership.getTeamRole() != TeamRole.MANAGER) {
            throw new ForbiddenOperationException("Only team owners or managers can perform this action");
        }
    }

    /** Team OWNER only, or a global ADMIN. */
    public void requireOwnerAccess(UUID teamId, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenOperationException("You are not a member of this team"));
        if (membership.getTeamRole() != TeamRole.OWNER) {
            throw new ForbiddenOperationException("Only the team owner can perform this action");
        }
    }

    public boolean isTeamMember(UUID teamId, UUID userId) {
        return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
    }
}
