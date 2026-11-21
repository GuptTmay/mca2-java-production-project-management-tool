package com.taskflow.task;

import com.taskflow.activity.ActivityService;
import com.taskflow.activity.ActivityType;
import com.taskflow.common.dto.PageResponse;
import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.InvalidStateTransitionException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.notification.NotificationService;
import com.taskflow.notification.NotificationType;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.project.ProjectRepository;
import com.taskflow.task.dto.AssignTaskRequest;
import com.taskflow.task.dto.CreateTaskRequest;
import com.taskflow.task.dto.TaskResponse;
import com.taskflow.task.dto.UpdateTaskRequest;
import com.taskflow.task.dto.UpdateTaskStatusRequest;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final TaskMapper taskMapper;
    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskHistoryMapper taskHistoryMapper;

    public TaskService(
            TaskRepository taskRepository,
            LabelRepository labelRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ProjectAccessGuard projectAccessGuard,
            TaskMapper taskMapper,
            ActivityService activityService,
            NotificationService notificationService,
            TaskHistoryRepository taskHistoryRepository,
            TaskHistoryMapper taskHistoryMapper) {
        this.taskRepository = taskRepository;
        this.labelRepository = labelRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.taskMapper = taskMapper;
        this.activityService = activityService;
        this.notificationService = notificationService;
        this.taskHistoryRepository = taskHistoryRepository;
        this.taskHistoryMapper = taskHistoryMapper;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, User currentUser) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> ResourceNotFoundException.of("Project", request.projectId()));

        projectAccessGuard.requireManageAccess(project, currentUser);

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = resolveAssignee(project, request.assigneeId());
        }

        Task task = new Task(
                project,
                request.title().trim(),
                request.description(),
                TaskStatus.TODO,
                request.priority() != null ? request.priority() : TaskPriority.MEDIUM,
                assignee,
                currentUser,
                request.deadline());

        TaskResponse response = taskMapper.toResponse(taskRepository.save(task));
        activityService.record(project.getId(), response.id(), currentUser, ActivityType.TASK_CREATED,
                currentUser.getFirstName() + " " + currentUser.getLastName() + " created this task", null);
        return response;
    }

    public TaskResponse getTask(UUID taskId, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        projectAccessGuard.requireViewAccess(task.getProject(), currentUser);
        return taskMapper.toResponse(task);
    }

    public PageResponse<TaskResponse> listTasks(TaskFilter filter, User currentUser, Pageable pageable) {
        if (filter.projectId() == null) {
            throw new IllegalArgumentException("projectId is required");
        }
        Project project = projectRepository.findById(filter.projectId())
                .orElseThrow(() -> ResourceNotFoundException.of("Project", filter.projectId()));
        projectAccessGuard.requireViewAccess(project, currentUser);

        var page = taskRepository.findAll(TaskSpecifications.fromFilter(filter), pageable);
        return PageResponse.from(page.map(taskMapper::toResponse));
    }

    /** Board view: same access rule as listTasks, grouping by status is a frontend concern. */
    public PageResponse<TaskResponse> getBoard(UUID projectId, User currentUser, Pageable pageable) {
        return listTasks(new TaskFilter(projectId, null, null, null, null, null, null), currentUser, pageable);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        requireEditAccess(task, currentUser);

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDeadline(request.deadline());

        TaskResponse response = taskMapper.toResponse(task);
        activityService.record(task.getProject().getId(), taskId, currentUser, ActivityType.TASK_UPDATED,
                currentUser.getFirstName() + " " + currentUser.getLastName() + " updated this task", null);
        return response;
    }

    /** Enforces the task workflow and each business role's permitted transitions. */
    @Transactional
    public TaskResponse updateStatus(UUID taskId, UpdateTaskStatusRequest request, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        projectAccessGuard.requireViewAccess(task.getProject(), currentUser);

        TaskStatus oldStatus = task.getStatus();
        TaskStatus targetStatus = request.status();

        TaskStatusTransitionValidator.validate(oldStatus, targetStatus);

        boolean assignedToCaller = task.getAssignee() != null
                && task.getAssignee().getId().equals(currentUser.getId());
        TaskStatusTransitionValidator.validateRolePermission(
                oldStatus, targetStatus, currentUser.getRole(), assignedToCaller);

        task.setStatus(targetStatus);

        TaskHistory history = new TaskHistory(
                taskId,
                task.getProject().getId(),
                currentUser,
                oldStatus,
                targetStatus,
                null
        );
        taskHistoryRepository.save(history);

        TaskResponse response = taskMapper.toResponse(task);
        activityService.record(task.getProject().getId(), taskId, currentUser, ActivityType.TASK_STATUS_CHANGED,
                currentUser.getFirstName() + " " + currentUser.getLastName() + " changed status from " + oldStatus + " to " + targetStatus, null);
        return response;
    }

    /**
     * Reassignment is a Producer action, not something an assignee can do to themselves.
     */
    @Transactional
    public TaskResponse assignTask(UUID taskId, AssignTaskRequest request, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        projectAccessGuard.requireManageAccess(task.getProject(), currentUser);

        User previousAssignee = task.getAssignee();

        if (request.assigneeId() == null) {
            task.setAssignee(null);
        } else {
            task.setAssignee(resolveAssignee(task.getProject(), request.assigneeId()));
        }

        TaskResponse response = taskMapper.toResponse(task);

        User newAssignee = task.getAssignee();
        if (newAssignee != null && !newAssignee.getId().equals(previousAssignee != null ? previousAssignee.getId() : null)) {
            activityService.record(task.getProject().getId(), taskId, currentUser, ActivityType.TASK_ASSIGNED,
                    currentUser.getFirstName() + " assigned this task to " + newAssignee.getFirstName() + " " + newAssignee.getLastName(), null);
            notificationService.create(newAssignee.getId(), NotificationType.TASK_ASSIGNED,
                    currentUser.getFirstName() + " assigned you to task: " + task.getTitle(), task.getId());
        }

        return response;
    }

    @Transactional
    public void deleteTask(UUID taskId, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        projectAccessGuard.requireManageAccess(task.getProject(), currentUser);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse addLabel(UUID taskId, UUID labelId, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        requireEditAccess(task, currentUser);

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> ResourceNotFoundException.of("Label", labelId));
        task.getLabels().add(label);

        TaskResponse response = taskMapper.toResponse(task);
        activityService.record(task.getProject().getId(), taskId, currentUser, ActivityType.LABEL_ADDED,
                currentUser.getFirstName() + " " + currentUser.getLastName() + " added label: " + label.getName(), null);
        return response;
    }

    @Transactional
    public TaskResponse removeLabel(UUID taskId, UUID labelId, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        requireEditAccess(task, currentUser);

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> ResourceNotFoundException.of("Label", labelId));
        task.getLabels().removeIf(l -> l.getId().equals(labelId));

        TaskResponse response = taskMapper.toResponse(task);
        activityService.record(task.getProject().getId(), taskId, currentUser, ActivityType.LABEL_REMOVED,
                currentUser.getFirstName() + " " + currentUser.getLastName() + " removed label: " + label.getName(), null);
        return response;
    }

    private Task findTaskOrThrow(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> ResourceNotFoundException.of("Task", taskId));
    }

    private User resolveAssignee(Project project, UUID assigneeId) {
        User candidate = userRepository.findById(assigneeId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", assigneeId));
        if (!candidate.isEnabled()) {
            throw new IllegalArgumentException("Tasks can only be assigned to active users");
        }
        if (!projectAccessGuard.isProjectMember(project, candidate.getId())) {
            throw new IllegalArgumentException("A task can only be assigned to a member of its project");
        }
        return candidate;
    }

    /** Field-level edit (title/description/priority/deadline/labels): Producer only. */
    private void requireEditAccess(Task task, User currentUser) {
        projectAccessGuard.requireManageAccess(task.getProject(), currentUser);
    }
}
