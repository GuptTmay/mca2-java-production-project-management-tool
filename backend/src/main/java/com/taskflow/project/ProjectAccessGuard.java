package com.taskflow.project;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.team.TeamAccessGuard;
import com.taskflow.user.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralizes project-level resource-level authorization so that any service
 * touching project-owned resources (projects themselves, and tasks) enforces
 * the exact same rules.
 */
@Component
public class ProjectAccessGuard {

    private final ProjectMemberRepository projectMemberRepository;
    private final TeamAccessGuard teamAccessGuard;

    public ProjectAccessGuard(ProjectMemberRepository projectMemberRepository, TeamAccessGuard teamAccessGuard) {
        this.projectMemberRepository = projectMemberRepository;
        this.teamAccessGuard = teamAccessGuard;
    }

    /** A current member of the project's team. */
    public void requireViewAccess(Project project, User currentUser) {
        try {
            teamAccessGuard.requireMembership(project.getTeam().getId(), currentUser);
        } catch (ForbiddenOperationException ex) {
            throw new ForbiddenOperationException("You do not have access to this project");
        }
    }

    /** A Producer who is a team OWNER/MANAGER - project membership alone is not enough. */
    public void requireManageAccess(Project project, User currentUser) {
        teamAccessGuard.requireManageAccess(project.getTeam().getId(), currentUser);
    }

    public boolean isProjectMember(Project project, UUID userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)
                && teamAccessGuard.isTeamMember(project.getTeam().getId(), userId);
    }
}
