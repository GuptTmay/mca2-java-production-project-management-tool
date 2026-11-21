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
import static org.mockito.Mockito.doThrow;
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
    void requireViewAccess_requiresTeamMembershipForProducers() {
        User producer = user(Role.PRODUCER);
        Project project = project(producer);

        assertDoesNotThrow(() -> guard.requireViewAccess(project, producer));
        verify(teamAccessGuard).requireMembership(project.getTeam().getId(), producer);
    }

    @Test
    void requireViewAccess_requiresCurrentTeamMembershipEvenForProjectMembers() {
        User removedMember = user(Role.TEAM_MEMBER);
        Project project = project(removedMember);
        doThrow(new ForbiddenOperationException("nope"))
                .when(teamAccessGuard).requireMembership(project.getTeam().getId(), removedMember);

        assertThrows(ForbiddenOperationException.class, () -> guard.requireViewAccess(project, removedMember));
        verify(teamAccessGuard).requireMembership(project.getTeam().getId(), removedMember);
    }

    @Test
    void requireViewAccess_allowsCurrentTeamMember() {
        User teamMember = user(Role.TEAM_MEMBER);
        Project project = project(teamMember);

        assertDoesNotThrow(() -> guard.requireViewAccess(project, teamMember));
        verify(teamAccessGuard).requireMembership(project.getTeam().getId(), teamMember);
    }

    @Test
    void requireViewAccess_forbidsOutsider() {
        User outsider = user(Role.TEAM_MEMBER);
        Project project = project(outsider);
        doThrow(new ForbiddenOperationException("nope"))
                .when(teamAccessGuard).requireMembership(project.getTeam().getId(), outsider);

        assertThrows(ForbiddenOperationException.class, () -> guard.requireViewAccess(project, outsider));
    }

    @Test
    void isProjectMember_rejectsUsersWhoHaveBeenRemovedFromTheTeam() {
        User removedMember = user(Role.TEAM_MEMBER);
        Project project = project(removedMember);
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), removedMember.getId())).thenReturn(true);
        when(teamAccessGuard.isTeamMember(project.getTeam().getId(), removedMember.getId())).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertFalse(guard.isProjectMember(project, removedMember.getId()));
    }

    @Test
    void requireManageAccess_delegatesToTeamAccessGuard() {
        User manager = user(Role.TEAM_MEMBER);
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
