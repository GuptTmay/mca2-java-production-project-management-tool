package com.taskflow.expense.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProjectFinanceResponse(
        UUID projectId,
        BigDecimal budgetAmount,
        String currencyCode,
        BigDecimal totalExpenses,
        BigDecimal remainingBudget,
        List<ExpenseResponse> expenses
) {
}
