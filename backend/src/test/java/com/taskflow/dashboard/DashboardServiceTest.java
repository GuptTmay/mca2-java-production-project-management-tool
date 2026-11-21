package com.taskflow.dashboard;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.dashboard.dto.MyTasksSummaryResponse;
import com.taskflow.dashboard.dto.ProjectDashboardResponse;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.project.ProjectRepository;
import com.taskflow.project.ProjectStatus;
import com.taskflow.task.PriorityCount;
import com.taskflow.task.StatusCount;
import com.taskflow.task.TaskMapper;
import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskRepository;
import com.taskflow.task.TaskStatus;
import com.taskflow.team.Team;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectAccessGuard projectAccessGuard;
    @Mock
    private TaskMapper taskMapper;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(taskRepository, projectRepository, projectAccessGuard, taskMapper);
    }

    @Test
    void getProjectDashboard_requiresProjectViewAccess() {
        User outsider = user(Role.TEAM_MEMBER);
        Project project = project(outsider);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireViewAccess(project, outsider);

        assertThrows(ForbiddenOperationException.class,
                () -> dashboardService.getProjectDashboard(project.getId(), outsider));
    }

    @Test
    void getProjectDashboard_computesTotalsPercentageAndZeroFillsMissingBuckets() {
        User owner = user(Role.TEAM_MEMBER);
        Project project = project(owner);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(taskRepository.countByProjectId(project.getId())).thenReturn(10L);
        when(taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.DONE)).thenReturn(3L);
        when(taskRepository.countByProjectIdAndDeadlineBeforeAndStatusNot(
                eq(project.getId()), any(Instant.class), eq(TaskStatus.DONE))).thenReturn(2L);
        when(taskRepository.countByStatusForProject(project.getId())).thenReturn(List.of(
                new StatusCount(TaskStatus.DONE, 3L), new StatusCount(TaskStatus.TODO, 7L)));
        when(taskRepository.countByPriorityForProject(project.getId())).thenReturn(List.of(
                new PriorityCount(TaskPriority.HIGH, 10L)));

        ProjectDashboardResponse response = dashboardService.getProjectDashboard(project.getId(), owner);

        assertThat(response.totalTasks()).isEqualTo(10);
        assertThat(response.completedTasks()).isEqualTo(3);
        assertThat(response.pendingTasks()).isEqualTo(7);
        assertThat(response.overdueTasks()).isEqualTo(2);
        assertThat(response.completionPercentage()).isEqualTo(30.0);
        // Statuses/priorities with no tasks still appear, at 0 - not just omitted.
        assertThat(response.tasksByStatus()).containsEntry(TaskStatus.IN_PROGRESS, 0L);
        assertThat(response.tasksByStatus()).containsEntry(TaskStatus.DONE, 3L);
        assertThat(response.tasksByPriority()).containsEntry(TaskPriority.LOW, 0L);
        assertThat(response.tasksByPriority()).containsEntry(TaskPriority.HIGH, 10L);
    }

    @Test
    void getProjectDashboard_avoidsDivisionByZeroWhenProjectHasNoTasks() {
        User owner = user(Role.TEAM_MEMBER);
        Project project = project(owner);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(taskRepository.countByProjectId(project.getId())).thenReturn(0L);
        when(taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.DONE)).thenReturn(0L);
        when(taskRepository.countByProjectIdAndDeadlineBeforeAndStatusNot(
                eq(project.getId()), any(Instant.class), eq(TaskStatus.DONE))).thenReturn(0L);
        when(taskRepository.countByStatusForProject(project.getId())).thenReturn(List.of());
        when(taskRepository.countByPriorityForProject(project.getId())).thenReturn(List.of());

        ProjectDashboardResponse response = dashboardService.getProjectDashboard(project.getId(), owner);

        assertThat(response.completionPercentage()).isEqualTo(0.0);
        assertThat(response.totalTasks()).isZero();
    }

    @Test
    void getMyTasksSummary_aggregatesAcrossProjectsForTheCurrentUserOnly() {
        User currentUser = user(Role.TEAM_MEMBER);
        when(taskRepository.countByAssigneeId(currentUser.getId())).thenReturn(5L);
        when(taskRepository.countByAssigneeIdAndDeadlineBeforeAndStatusNot(
                eq(currentUser.getId()), any(Instant.class), eq(TaskStatus.DONE))).thenReturn(1L);
        when(taskRepository.countByStatusForAssignee(currentUser.getId())).thenReturn(List.of(
                new StatusCount(TaskStatus.TODO, 5L)));
        Page<com.taskflow.task.Task> emptyPage = new PageImpl<>(List.of());
        when(taskRepository.findByAssigneeId(eq(currentUser.getId()), any(Pageable.class))).thenReturn(emptyPage);

        MyTasksSummaryResponse response = dashboardService.getMyTasksSummary(currentUser, Pageable.unpaged());

        assertThat(response.totalAssignedTasks()).isEqualTo(5);
        assertThat(response.overdueTasks()).isEqualTo(1);
        assertThat(response.tasksByStatus()).containsEntry(TaskStatus.TODO, 5L);
        assertThat(response.tasksByStatus()).containsEntry(TaskStatus.DONE, 0L);
    }

    // --- Test helpers ---

    private static User user(Role role) {
        User u = new User(role.name().toLowerCase() + UUID.randomUUID() + "@taskflow.io", "hash", "First", "Last", role);
        setId(u, User.class, UUID.randomUUID());
        return u;
    }

    private static Project project(User createdBy) {
        Team team = new Team("Platform", "desc", createdBy);
        setId(team, Team.class, UUID.randomUUID());
        Project project = new Project(team, "Website Revamp", "desc", ProjectStatus.ACTIVE, null, null, createdBy);
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
