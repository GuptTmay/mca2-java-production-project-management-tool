package com.taskflow.dashboard;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.dashboard.dto.MyTasksSummaryResponse;
import com.taskflow.dashboard.dto.ProjectDashboardResponse;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.project.ProjectRepository;
import com.taskflow.task.PriorityCount;
import com.taskflow.task.StatusCount;
import com.taskflow.task.TaskMapper;
import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskRepository;
import com.taskflow.task.TaskStatus;
import com.taskflow.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final TaskMapper taskMapper;

    public DashboardService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            ProjectAccessGuard projectAccessGuard,
            TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.taskMapper = taskMapper;
    }

    public ProjectDashboardResponse getProjectDashboard(UUID projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", projectId));
        projectAccessGuard.requireViewAccess(project, currentUser);

        long total = taskRepository.countByProjectId(projectId);
        long completed = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE);
        long pending = total - completed;
        long overdue = taskRepository.countByProjectIdAndDeadlineBeforeAndStatusNot(
                projectId, Instant.now(), TaskStatus.DONE);

        Map<TaskStatus, Long> byStatus = zeroFilledStatusMap();
        for (StatusCount sc : taskRepository.countByStatusForProject(projectId)) {
            byStatus.put(sc.status(), sc.count());
        }

        Map<TaskPriority, Long> byPriority = zeroFilledPriorityMap();
        for (PriorityCount pc : taskRepository.countByPriorityForProject(projectId)) {
            byPriority.put(pc.priority(), pc.count());
        }

        return new ProjectDashboardResponse(
                projectId, total, completed, pending, overdue, byStatus, byPriority,
                completionPercentage(completed, total));
    }

    public MyTasksSummaryResponse getMyTasksSummary(User currentUser, Pageable pageable) {
        UUID userId = currentUser.getId();

        long total = taskRepository.countByAssigneeId(userId);
        long overdue = taskRepository.countByAssigneeIdAndDeadlineBeforeAndStatusNot(
                userId, Instant.now(), TaskStatus.DONE);

        Map<TaskStatus, Long> byStatus = zeroFilledStatusMap();
        for (StatusCount sc : taskRepository.countByStatusForAssignee(userId)) {
            byStatus.put(sc.status(), sc.count());
        }

        PageResponse<com.taskflow.task.dto.TaskResponse> tasks =
                PageResponse.from(taskRepository.findByAssigneeId(userId, pageable).map(taskMapper::toResponse));

        return new MyTasksSummaryResponse(total, overdue, byStatus, tasks);
    }

    private static double completionPercentage(long completed, long total) {
        if (total == 0) {
            return 0.0;
        }
        // Rounded to one decimal place for a stable, presentable dashboard value.
        return Math.round(completed * 1000.0 / total) / 10.0;
    }

    private static Map<TaskStatus, Long> zeroFilledStatusMap() {
        Map<TaskStatus, Long> map = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            map.put(status, 0L);
        }
        return map;
    }

    private static Map<TaskPriority, Long> zeroFilledPriorityMap() {
        Map<TaskPriority, Long> map = new EnumMap<>(TaskPriority.class);
        for (TaskPriority priority : TaskPriority.values()) {
            map.put(priority, 0L);
        }
        return map;
    }
}
