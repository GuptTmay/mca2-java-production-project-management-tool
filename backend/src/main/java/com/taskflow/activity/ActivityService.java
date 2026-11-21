package com.taskflow.activity;

import com.taskflow.activity.dto.ActivityResponse;
import com.taskflow.activity.ActivityType;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.project.ProjectRepository;
import com.taskflow.task.Task;
import com.taskflow.task.TaskRepository;
import com.taskflow.user.User;
import com.taskflow.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final ActivityMapper activityMapper;

    public ActivityService(
            ActivityRepository activityRepository,
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            ProjectAccessGuard projectAccessGuard,
            ActivityMapper activityMapper) {
        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.activityMapper = activityMapper;
    }

    public List<ActivityResponse> listForTask(UUID taskId, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        projectAccessGuard.requireViewAccess(task.getProject(), currentUser);
        return activityRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(activityMapper::toResponse)
                .toList();
    }

    public List<ActivityResponse> listForProject(UUID projectId, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        projectAccessGuard.requireViewAccess(project, currentUser);
        return activityRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(activityMapper::toResponse)
                .toList();
    }

    @Transactional
    public void record(UUID projectId, UUID taskId, User actor, ActivityType type, String description, String metadata) {
        Activity activity = new Activity(taskId, projectId, actor, type, description, metadata);
        activityRepository.save(activity);
    }

    private Task findTaskOrThrow(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> ResourceNotFoundException.of("Task", taskId));
    }

    private Project findProjectOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", projectId));
    }
}
