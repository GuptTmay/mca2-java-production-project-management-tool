package com.taskflow.task;

import com.taskflow.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskStatusTransitionValidatorTest {

    @ParameterizedTest
    @CsvSource({
            "TODO, IN_PROGRESS",
            "IN_PROGRESS, TODO",
            "IN_PROGRESS, IN_REVIEW",
            "IN_REVIEW, IN_PROGRESS",
            "IN_REVIEW, DONE",
            "DONE, IN_PROGRESS"
    })
    void validate_allowsAdjacentColumnMoves(TaskStatus from, TaskStatus to) {
        assertDoesNotThrow(() -> TaskStatusTransitionValidator.validate(from, to));
    }

    @ParameterizedTest
    @CsvSource({
            "TODO, IN_REVIEW",
            "TODO, DONE",
            "IN_PROGRESS, DONE",
            "DONE, TODO",
            "IN_REVIEW, TODO"
    })
    void validate_rejectsSkippingColumns(TaskStatus from, TaskStatus to) {
        assertThrows(InvalidStateTransitionException.class, () -> TaskStatusTransitionValidator.validate(from, to));
    }

    @Test
    void validate_allowsDroppingBackInTheSameColumn() {
        for (TaskStatus status : TaskStatus.values()) {
            assertDoesNotThrow(() -> TaskStatusTransitionValidator.validate(status, status));
        }
    }
}
