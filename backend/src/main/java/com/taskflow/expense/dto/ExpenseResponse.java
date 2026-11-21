package com.taskflow.expense.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID projectId,
        String description,
        String category,
        BigDecimal amount,
        LocalDate spentOn,
        UUID createdBy,
        Instant createdAt
) {
}
