package com.taskflow.project;

import com.taskflow.common.exception.DuplicateResourceException;
import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.project.dto.AddProjectMemberRequest;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.team.Team;
import com.taskflow.team.TeamAccessGuard;
import com.taskflow.team.TeamRepository;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Authorization pass/fail behavior itself is covered in TeamAccessGuardTest and
 * ProjectAccessGuardTest. These tests mock both guards and focus on what
 * ProjectService does once access is granted or denied: correct delegation,
 * and business rules (date validation, default status, team-membership
 * precondition for project membership, duplicate-member rejection).
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamAccessGuard teamAccessGuard;
    @Mock
    private ProjectAccessGuard projectAccessGuard;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectMemberMapper projectMemberMapper;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository, projectMemberRepository, teamRepository, userRepository,
                teamAccessGuard, projectAccessGuard, projectMapper, projectMemberMapper);
    }

    @Test
    void createProject_requiresTeamManageAccess() {
        User outsider = withUserId(new User("out@taskflow.io", "hash", "Out", "Sider", Role.DEVELOPER));
        UUID teamId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest(teamId, "Website Revamp", "desc", null, null, null);
        doThrow(new ForbiddenOperationException("nope"))
                .when(teamAccessGuard).requireManageAccess(teamId, outsider);

        assertThrows(ForbiddenOperationException.class, () -> projectService.createProject(request, outsider));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_addsCreatorAsProjectMemberAndDefaultsStatusToPlanning() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        CreateProjectRequest request = new CreateProjectRequest(team.getId(), "Website Revamp", "desc", null, null, null);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectMapper.toResponse(any())).thenReturn(dummyResponse());

        projectService.createProject(request, owner);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getStatus()).isEqualTo(ProjectStatus.PLANNING);

        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getUser()).isEqualTo(owner);
    }

    @Test
    void createProject_rejectsEndDateBeforeStartDate() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        UUID teamId = UUID.randomUUID();
        CreateProjectRequest request = new CreateProjectRequest(
                teamId, "Website Revamp", "desc", null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1));

        assertThrows(IllegalArgumentException.class, () -> projectService.createProject(request, owner));
    }

    @Test
    void getProject_delegatesViewCheckToGuard() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        Project project = withProjectId(new Project(
                team, "Website Revamp", "desc", ProjectStatus.ACTIVE, null, null, owner));

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(dummyResponse());

        projectService.getProject(project.getId(), owner);

        verify(projectAccessGuard).requireViewAccess(project, owner);
    }

    @Test
    void getProject_propagatesGuardRejection() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        User outsider = withUserId(new User("outsider@taskflow.io", "hash", "Out", "Sider", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        Project project = withProjectId(new Project(
                team, "Website Revamp", "desc", ProjectStatus.ACTIVE, null, null, owner));

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireViewAccess(project, outsider);

        assertThrows(ForbiddenOperationException.class, () -> projectService.getProject(project.getId(), outsider));
    }

    @Test
    void addMember_rejectsUserWhoIsNotOnTheTeam() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        User outsider = withUserId(new User("outsider@taskflow.io", "hash", "Out", "Sider", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        Project project = withProjectId(new Project(
                team, "Website Revamp", "desc", ProjectStatus.ACTIVE, null, null, owner));

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(outsider.getEmail())).thenReturn(Optional.of(outsider));
        when(teamAccessGuard.isTeamMember(team.getId(), outsider.getId())).thenReturn(false);

        AddProjectMemberRequest request = new AddProjectMemberRequest(outsider.getEmail());

        assertThrows(IllegalArgumentException.class,
                () -> projectService.addMember(project.getId(), request, owner));
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void addMember_rejectsDuplicateProjectMembership() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        User teammate = withUserId(new User("teammate@taskflow.io", "hash", "Team", "Mate", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        Project project = withProjectId(new Project(
                team, "Website Revamp", "desc", ProjectStatus.ACTIVE, null, null, owner));

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findByEmail(teammate.getEmail())).thenReturn(Optional.of(teammate));
        when(teamAccessGuard.isTeamMember(team.getId(), teammate.getId())).thenReturn(true);
        when(projectMemberRepository.existsByProjectIdAndUserId(project.getId(), teammate.getId())).thenReturn(true);

        AddProjectMemberRequest request = new AddProjectMemberRequest(teammate.getEmail());

        assertThrows(DuplicateResourceException.class,
                () -> projectService.addMember(project.getId(), request, owner));
    }

    // --- Test helpers ---

    private static User withUserId(User user) {
        setId(user, User.class, UUID.randomUUID());
        return user;
    }

    private static Team withTeamId(Team team) {
        setId(team, Team.class, UUID.randomUUID());
        return team;
    }

    private static Project withProjectId(Project project) {
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

    private static com.taskflow.project.dto.ProjectResponse dummyResponse() {
        return new com.taskflow.project.dto.ProjectResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Website Revamp", "desc",
                ProjectStatus.PLANNING, null, null, UUID.randomUUID(),
                java.time.Instant.now(), java.time.Instant.now());
    }
}
