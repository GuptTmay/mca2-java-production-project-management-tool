package com.taskflow.project;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.team.TeamAccessGuard;
import com.taskflow.user.Role;
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

    /**
     * Explicit project member, any member of the project's team, or a global ADMIN.
     */
    public void requireViewAccess(Project project, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), currentUser.getId())) {
            return;
        }
        try {
            teamAccessGuard.requireMembership(project.getTeam().getId(), currentUser);
        } catch (ForbiddenOperationException ex) {
            throw new ForbiddenOperationException("You do not have access to this project");
        }
    }

    /** Team OWNER/MANAGER, or a global ADMIN - project membership alone is not enough. */
    public void requireManageAccess(Project project, User currentUser) {
        teamAccessGuard.requireManageAccess(project.getTeam().getId(), currentUser);
    }

    public boolean isProjectMember(UUID projectId, UUID userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }
}
