package com.taskflow.project;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.team.Team;
import com.taskflow.team.TeamAccessGuard;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAccessGuardTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private TeamAccessGuard teamAccessGuard;

    private ProjectAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ProjectAccessGuard(projectMemberRepository, teamAccessGuard);
    }

    @Test
    void requireViewAccess_allowsAdminWithoutAnyLookup() {
        User admin = user(Role.ADMIN);
        Project project = project(admin);

        assertDoesNotThrow(() -> guard.requireViewAccess(project, admin));
        verify(projectMemberRepository, never()).existsByProjectIdAndUserId(any(), any());
    }

    @Test
    void requireViewAccess_allowsExplicitProjectMember() {
        User dev = user(Role.DEVELOPER);
        Project project = project(dev);
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), dev.getId())).thenReturn(true);

        assertDoesNotThrow(() -> guard.requireViewAccess(project, dev));
        verify(teamAccessGuard, never()).requireManageAccess(any(), any());
    }

    @Test
    void requireViewAccess_fallsBackToTeamManageAccessWhenNotAProjectMember() {
        User manager = user(Role.DEVELOPER);
        Project project = project(manager);
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), manager.getId())).thenReturn(false);
        // teamAccessGuard.requireManageAccess is a no-op mock by default => team-level access granted

        assertDoesNotThrow(() -> guard.requireViewAccess(project, manager));
        verify(teamAccessGuard).requireManageAccess(project.getTeam().getId(), manager);
    }

    @Test
    void requireViewAccess_forbidsUserWhoIsNeitherProjectMemberNorTeamManager() {
        User outsider = user(Role.DEVELOPER);
        Project project = project(outsider);
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), outsider.getId())).thenReturn(false);
        doThrow(new ForbiddenOperationException("nope"))
                .when(teamAccessGuard).requireManageAccess(project.getTeam().getId(), outsider);

        assertThrows(ForbiddenOperationException.class, () -> guard.requireViewAccess(project, outsider));
    }

    @Test
    void requireManageAccess_delegatesToTeamAccessGuard() {
        User manager = user(Role.DEVELOPER);
        Project project = project(manager);

        guard.requireManageAccess(project, manager);

        verify(teamAccessGuard).requireManageAccess(project.getTeam().getId(), manager);
    }

    private static User user(Role role) {
        User u = new User(role.name().toLowerCase() + "@taskflow.io", "hash", "First", "Last", role);
        setId(u, User.class, UUID.randomUUID());
        return u;
    }

    private static Project project(User createdBy) {
        Team team = new Team("Platform", "desc", createdBy);
        setId(team, Team.class, UUID.randomUUID());
        Project project = new Project(
                team, "Website Revamp", "desc", ProjectStatus.ACTIVE, null, null, createdBy);
        setId(project, Project.class, UUID.randomUUID());
        return project;
    }

    private static void setId(Object target, Class<?> type, UUID id) {
        try {
            Field idField = type.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
