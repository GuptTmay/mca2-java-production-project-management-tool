package com.taskflow.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateProjectBudgetRequest(
        @NotNull @DecimalMin(value = "0.00") @Digits(integer = 17, fraction = 2) BigDecimal budgetAmount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "currencyCode must be a three-letter ISO code")
        String currencyCode
) {
}
