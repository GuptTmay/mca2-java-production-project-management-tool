package com.taskflow.dashboard.dto;

import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskStatus;

import java.util.Map;
import java.util.UUID;

public record ProjectDashboardResponse(
        UUID projectId,
        long totalTasks,
        long completedTasks,
        long pendingTasks,
        long overdueTasks,
        Map<TaskStatus, Long> tasksByStatus,
        Map<TaskPriority, Long> tasksByPriority,
        double completionPercentage
) {
}
