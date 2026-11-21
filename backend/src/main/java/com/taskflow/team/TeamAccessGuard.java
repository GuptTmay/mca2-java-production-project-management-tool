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

    /** Any member of the team. */
    public void requireMembership(UUID teamId, User currentUser) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, currentUser.getId())) {
            throw new ForbiddenOperationException("You are not a member of this team");
        }
    }

    /** A Producer who is a team OWNER or MANAGER. */
    public void requireManageAccess(UUID teamId, User currentUser) {
        if (currentUser.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Only Producers can manage teams and projects");
        }
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUser.getId())
                .orElseThrow(() -> new ForbiddenOperationException("You are not a member of this team"));
        if (membership.getTeamRole() != TeamRole.OWNER && membership.getTeamRole() != TeamRole.MANAGER) {
            throw new ForbiddenOperationException("Only team owners or managers can perform this action");
        }
    }

    /** A Producer who is the team OWNER. */
    public void requireOwnerAccess(UUID teamId, User currentUser) {
        if (currentUser.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Only Producers can manage teams");
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
