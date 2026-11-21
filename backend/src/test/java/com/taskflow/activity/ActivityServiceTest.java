package com.taskflow.activity;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.activity.dto.ActivityResponse;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.project.ProjectRepository;
import com.taskflow.project.ProjectStatus;
import com.taskflow.task.Task;
import com.taskflow.task.TaskRepository;
import com.taskflow.team.Team;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectAccessGuard projectAccessGuard;
    @Mock
    private ActivityMapper activityMapper;

    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        activityService = new ActivityService(
                activityRepository, taskRepository, projectRepository, projectAccessGuard, activityMapper);
    }

    @Test
    void listForTask_requiresProjectViewAccess() {
        User outsider = user(Role.TEAM_MEMBER);
        Task task = task(outsider);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireViewAccess(task.getProject(), outsider);

        assertThrows(ForbiddenOperationException.class,
                () -> activityService.listForTask(task.getId(), outsider));
        verify(activityRepository, never()).findByTaskIdOrderByCreatedAtDesc(any());
    }

    @Test
    void listForProject_requiresProjectViewAccess() {
        User outsider = user(Role.TEAM_MEMBER);
        Project project = project(outsider);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireViewAccess(project, outsider);

        assertThrows(ForbiddenOperationException.class,
                () -> activityService.listForProject(project.getId(), outsider));
        verify(activityRepository, never()).findByProjectIdOrderByCreatedAtDesc(any());
    }

    @Test
    void record_savesActivity() {
        User actor = user(Role.TEAM_MEMBER);
        UUID taskId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        activityService.record(projectId, taskId, actor, ActivityType.TASK_CREATED, "created", null);

        verify(activityRepository).save(any(Activity.class));
    }

    // --- Helpers ---

    private static User user(Role role) {
        User u = new User(role.name().toLowerCase() + UUID.randomUUID() + "@taskflow.io", "hash", "First", "Last", role);
        setId(u, User.class, UUID.randomUUID());
        return u;
    }

    private static Task task(User createdBy) {
        Team team = new Team("Platform", "desc", createdBy);
        setId(team, Team.class, UUID.randomUUID());
        Project project = new Project(
                team, "Website Revamp", "desc", com.taskflow.project.ProjectStatus.ACTIVE, null, null, createdBy);
        setId(project, Project.class, UUID.randomUUID());
        Task task = new Task(project, "Fix bug", "desc", com.taskflow.task.TaskStatus.TODO, com.taskflow.task.TaskPriority.MEDIUM, createdBy, createdBy, null);
        setId(task, Task.class, UUID.randomUUID());
        return task;
    }

    private static Project project(User createdBy) {
        Team team = new Team("Platform", "desc", createdBy);
        setId(team, Team.class, UUID.randomUUID());
        Project project = new Project(
                team, "Website Revamp", "desc", com.taskflow.project.ProjectStatus.ACTIVE, null, null, createdBy);
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
