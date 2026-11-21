package com.taskflow.dashboard;

import com.taskflow.dashboard.dto.MyTasksSummaryResponse;
import com.taskflow.dashboard.dto.ProjectDashboardResponse;
import com.taskflow.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/projects/{id}/dashboard")
    public ProjectDashboardResponse projectDashboard(
            @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return dashboardService.getProjectDashboard(id, currentUser);
    }

    @GetMapping("/dashboard/my-tasks")
    public MyTasksSummaryResponse myTasks(
            @AuthenticationPrincipal User currentUser, @PageableDefault(size = 20) Pageable pageable) {
        return dashboardService.getMyTasksSummary(currentUser, pageable);
    }
}
