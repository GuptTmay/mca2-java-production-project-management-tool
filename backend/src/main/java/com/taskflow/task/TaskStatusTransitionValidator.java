package com.taskflow.task;

import com.taskflow.common.exception.InvalidStateTransitionException;
import com.taskflow.team.TeamRole;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Role-based task status transition validator.
 *
 * Allowed transitions:
 *  - MEMBER: TODO -> IN_PROGRESS, IN_PROGRESS -> TODO, IN_PROGRESS -> IN_REVIEW
 *  - MANAGER/OWNER: all MEMBER transitions plus IN_REVIEW -> IN_PROGRESS, IN_REVIEW -> DONE, DONE -> IN_PROGRESS
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

    /**
     * Validates that the caller's team role permits the requested transition.
     *
     * @param current   current task status
     * @param target    requested new status
     * @param teamRole  caller's team role (OWNER, MANAGER, MEMBER)
     */
    public static void validateRolePermission(TaskStatus current, TaskStatus target, TeamRole teamRole) {
        if (current == target) {
            return;
        }
        if (teamRole == TeamRole.OWNER || teamRole == TeamRole.MANAGER) {
            return;
        }
        if (teamRole != TeamRole.MEMBER) {
            throw new InvalidStateTransitionException("Unknown team role: " + teamRole);
        }
        boolean memberAllowed = switch (current) {
            case TODO -> target == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == TaskStatus.TODO || target == TaskStatus.IN_REVIEW;
            case IN_REVIEW, DONE -> false;
        };
        if (!memberAllowed) {
            throw new InvalidStateTransitionException(
                    "Members can only move tasks from TODO to IN_PROGRESS, or from IN_PROGRESS to TODO or IN_REVIEW");
        }
    }
}
