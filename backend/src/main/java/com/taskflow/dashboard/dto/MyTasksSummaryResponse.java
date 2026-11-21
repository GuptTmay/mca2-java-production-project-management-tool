package com.taskflow.dashboard.dto;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.task.TaskStatus;
import com.taskflow.task.dto.TaskResponse;

import java.util.Map;

public record MyTasksSummaryResponse(
        long totalAssignedTasks,
        long overdueTasks,
        Map<TaskStatus, Long> tasksByStatus,
        PageResponse<TaskResponse> tasks
) {
}
