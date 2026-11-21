package com.taskflow.task;

import com.taskflow.activity.ActivityService;
import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.InvalidStateTransitionException;
import com.taskflow.notification.NotificationService;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.project.ProjectRepository;
import com.taskflow.team.Team;
import com.taskflow.team.TeamAccessGuard;
import com.taskflow.team.TeamMemberRepository;
import com.taskflow.task.TaskHistoryMapper;
import com.taskflow.task.TaskHistoryRepository;
import com.taskflow.task.dto.AssignTaskRequest;
import com.taskflow.task.dto.CreateTaskRequest;
import com.taskflow.task.dto.TaskResponse;
import com.taskflow.task.dto.UpdateTaskRequest;
import com.taskflow.task.dto.UpdateTaskStatusRequest;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
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
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private LabelRepository labelRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectAccessGuard projectAccessGuard;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private ActivityService activityService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TeamAccessGuard teamAccessGuard;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private TaskHistoryRepository taskHistoryRepository;
    @Mock
    private TaskHistoryMapper taskHistoryMapper;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository, labelRepository, projectRepository, userRepository, projectAccessGuard, taskMapper, activityService, notificationService, teamAccessGuard, teamMemberRepository, taskHistoryRepository, taskHistoryMapper);
    }

    @Test
    void createTask_requiresManageAccess() {
        User outsider = user(Role.DEVELOPER);
        Project project = project(outsider);
        CreateTaskRequest request = new CreateTaskRequest(project.getId(), "Fix bug", "desc", null, null, null);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireManageAccess(project, outsider);

        assertThrows(ForbiddenOperationException.class, () -> taskService.createTask(request, outsider));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_defaultsStatusToTodoAndPriorityToMedium() {
        User reporter = user(Role.DEVELOPER);
        Project project = project(reporter);
        CreateTaskRequest request = new CreateTaskRequest(project.getId(), "Fix bug", "desc", null, null, null);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskMapper.toResponse(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return new TaskResponse(t.getId(), t.getProject().getId(), t.getTitle(), t.getDescription(),
                    t.getStatus(), t.getPriority(), t.getAssignee() != null ? t.getAssignee().getId() : null,
                    t.getReporter().getId(), t.getDeadline(), java.util.Set.of(), 0L,
                    java.time.Instant.now(), java.time.Instant.now());
        });

        taskService.createTask(request, reporter);

        org.mockito.ArgumentCaptor<Task> captor = org.mockito.ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(captor.getValue().getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(captor.getValue().getReporter()).isEqualTo(reporter);
    }

    @Test
    void createTask_rejectsAssigneeWhoIsNotAProjectMember() {
        User reporter = user(Role.DEVELOPER);
        User notAMember = user(Role.DEVELOPER);
        Project project = project(reporter);
        CreateTaskRequest request = new CreateTaskRequest(
                project.getId(), "Fix bug", "desc", null, notAMember.getId(), null);

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(userRepository.findById(notAMember.getId())).thenReturn(Optional.of(notAMember));
        when(projectAccessGuard.isProjectMember(project.getId(), notAMember.getId())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> taskService.createTask(request, reporter));
    }

    @Test
    void updateTask_forbidsAssigneeFromEditing() {
        User assignee = user(Role.DEVELOPER);
        Project project = project(assignee);
        Task task = task(project, assignee, assignee);
        UpdateTaskRequest request = new UpdateTaskRequest("New title", "desc", TaskPriority.HIGH, null);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireManageAccess(project, assignee);

        assertThrows(ForbiddenOperationException.class, () -> taskService.updateTask(task.getId(), request, assignee));
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTask_forbidsAPlainProjectMemberWhoIsNotTheAssignee() {
        User assignee = user(Role.DEVELOPER);
        User otherMember = user(Role.DEVELOPER);
        Project project = project(assignee);
        Task task = task(project, assignee, assignee);
        UpdateTaskRequest request = new UpdateTaskRequest("New title", "desc", TaskPriority.HIGH, null);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireManageAccess(project, otherMember);

        assertThrows(ForbiddenOperationException.class,
                () -> taskService.updateTask(task.getId(), request, otherMember));
    }

    @Test
    void updateTask_allowsTeamManagerEvenWhenNotTheAssignee() {
        User assignee = user(Role.DEVELOPER);
        User manager = user(Role.DEVELOPER);
        Project project = project(assignee);
        Task task = task(project, assignee, assignee);
        UpdateTaskRequest request = new UpdateTaskRequest("New title", "desc", TaskPriority.HIGH, null);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        // projectAccessGuard.requireManageAccess is a no-op mock by default => manager access granted

        taskService.updateTask(task.getId(), request, manager);

        assertThat(task.getTitle()).isEqualTo("New title");
    }

    @Test
    void updateStatus_requiresManageAccess() {
        User assignee = user(Role.DEVELOPER);
        User otherMember = user(Role.DEVELOPER);
        Project project = project(assignee);
        Task task = task(project, assignee, assignee);
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireManageAccess(project, otherMember);

        assertThrows(ForbiddenOperationException.class,
                () -> taskService.updateStatus(task.getId(), request, otherMember));
    }

    @Test
    void updateStatus_rejectsIllegalColumnSkip() {
        User assignee = user(Role.DEVELOPER);
        Project project = project(assignee);
        Task task = task(project, assignee, assignee); // starts at TODO
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThrows(InvalidStateTransitionException.class,
                () -> taskService.updateStatus(task.getId(), request, assignee));
    }

    @Test
    void assignTask_requiresManageAccessEvenFromTheCurrentAssignee() {
        User assignee = user(Role.DEVELOPER);
        Project project = project(assignee);
        Task task = task(project, assignee, assignee);
        User newAssignee = user(Role.DEVELOPER);
        AssignTaskRequest request = new AssignTaskRequest(newAssignee.getId());

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireManageAccess(project, assignee);

        assertThrows(ForbiddenOperationException.class,
                () -> taskService.assignTask(task.getId(), request, assignee));
    }

    @Test
    void assignTask_unassignsWhenAssigneeIdIsNull() {
        User assignee = user(Role.DEVELOPER);
        User manager = user(Role.DEVELOPER);
        Project project = project(assignee);
        Task task = task(project, assignee, assignee);
        AssignTaskRequest request = new AssignTaskRequest(null);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        taskService.assignTask(task.getId(), request, manager);

        assertThat(task.getAssignee()).isNull();
    }

    @Test
    void listTasks_requiresProjectId() {
        TaskFilter filter = new TaskFilter(null, null, null, null, null, null, null);
        User user = user(Role.DEVELOPER);

        assertThrows(IllegalArgumentException.class,
                () -> taskService.listTasks(filter, user, org.springframework.data.domain.Pageable.unpaged()));
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
        Project project = new Project(
                team, "Website Revamp", "desc", com.taskflow.project.ProjectStatus.ACTIVE, null, null, createdBy);
        setId(project, Project.class, UUID.randomUUID());
        return project;
    }

    private static Task task(Project project, User assignee, User reporter) {
        Task task = new Task(project, "Fix bug", "desc", TaskStatus.TODO, TaskPriority.MEDIUM, assignee, reporter, null);
        setId(task, Task.class, UUID.randomUUID());
        return task;
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
