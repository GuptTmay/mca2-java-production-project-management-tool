package com.taskflow.task;

import com.taskflow.common.exception.InvalidStateTransitionException;
import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.user.Role;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Role-based task status transition validator.
 *
 * Allowed transitions:
 *  - Team Member: assigned task TODO -> IN_PROGRESS, IN_PROGRESS -> TODO/IN_REVIEW
 *  - Content Manager: IN_REVIEW -> IN_PROGRESS/DONE and DONE -> IN_PROGRESS
 *  - Producer: any adjacent transition
 *
 * Any transition not listed below is rejected with a 400 Bad Request.
 * Attempting a legal transition without the required role is rejected with 403 Forbidden.
 */
public final class TaskStatusTransitionValidator {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(TaskStatus.TODO, EnumSet.of(TaskStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.TODO, TaskStatus.IN_REVIEW));
        ALLOWED_TRANSITIONS.put(TaskStatus.IN_REVIEW, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.DONE));
        ALLOWED_TRANSITIONS.put(TaskStatus.DONE, EnumSet.of(TaskStatus.IN_PROGRESS));
    }

    private TaskStatusTransitionValidator() {
    }

    /**
     * Validates that the requested transition is structurally possible
     * (adjacent columns, same status is a no-op).
     */
    public static void validate(TaskStatus current, TaskStatus target) {
        if (current == target) {
            return;
        }
        Set<TaskStatus> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new InvalidStateTransitionException(
                    "Cannot move a task from " + current + " directly to " + target);
        }
    }

    public static void validateRolePermission(
            TaskStatus current, TaskStatus target, Role role, boolean assignedToCaller) {
        if (current == target || role == Role.PRODUCER) {
            return;
        }

        if (role == Role.CONTENT_MANAGER) {
            boolean mayReview = switch (current) {
                case IN_REVIEW -> target == TaskStatus.IN_PROGRESS || target == TaskStatus.DONE;
                case DONE -> target == TaskStatus.IN_PROGRESS;
                default -> false;
            };
            if (mayReview) {
                return;
            }
            throw new ForbiddenOperationException("Content Managers can only approve or return tasks for review");
        }

        if (!assignedToCaller) {
            throw new ForbiddenOperationException("Team Members can only update tasks assigned to them");
        }
        boolean memberAllowed = switch (current) {
            case TODO -> target == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == TaskStatus.TODO || target == TaskStatus.IN_REVIEW;
            case IN_REVIEW, DONE -> false;
        };
        if (!memberAllowed) {
            throw new ForbiddenOperationException("Team Members can only start, pause, or submit their assigned tasks for review");
        }
    }
}
