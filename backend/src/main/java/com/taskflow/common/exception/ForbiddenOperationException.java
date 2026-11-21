package com.taskflow.common.exception;

/**
 * Thrown for resource-level authorization failures — the caller is authenticated
 * and holds a role that could permit the action in general, but is not permitted
 * to act on this specific resource (e.g. a DEVELOPER updating a task assigned to
 * someone else). Distinct from Spring Security's role-based AccessDeniedException,
 * which the GlobalExceptionHandler also maps to 403.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
