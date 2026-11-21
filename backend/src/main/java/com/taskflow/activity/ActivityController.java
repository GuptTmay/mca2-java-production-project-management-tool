package com.taskflow.activity;

import com.taskflow.activity.dto.ActivityResponse;
import com.taskflow.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/tasks/{taskId}")
    public List<ActivityResponse> listForTask(@PathVariable UUID taskId, @AuthenticationPrincipal User currentUser) {
        return activityService.listForTask(taskId, currentUser);
    }

    @GetMapping("/projects/{projectId}")
    public List<ActivityResponse> listForProject(@PathVariable UUID projectId, @AuthenticationPrincipal User currentUser) {
        return activityService.listForProject(projectId, currentUser);
    }
}
