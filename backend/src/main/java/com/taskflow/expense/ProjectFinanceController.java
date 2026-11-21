package com.taskflow.expense;

import com.taskflow.expense.dto.CreateExpenseRequest;
import com.taskflow.expense.dto.ProjectFinanceResponse;
import com.taskflow.expense.dto.UpdateProjectBudgetRequest;
import com.taskflow.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
public class ProjectFinanceController {

    private final ProjectFinanceService projectFinanceService;

    public ProjectFinanceController(ProjectFinanceService projectFinanceService) {
        this.projectFinanceService = projectFinanceService;
    }

    @GetMapping("/finance")
    public ProjectFinanceResponse getFinance(
            @PathVariable UUID projectId, @AuthenticationPrincipal User currentUser) {
        return projectFinanceService.getFinance(projectId, currentUser);
    }

    @PatchMapping("/budget")
    public ProjectFinanceResponse updateBudget(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectBudgetRequest request,
            @AuthenticationPrincipal User currentUser) {
        return projectFinanceService.updateBudget(projectId, request, currentUser);
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectFinanceResponse addExpense(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal User currentUser) {
        return projectFinanceService.addExpense(projectId, request, currentUser);
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ProjectFinanceResponse deleteExpense(
            @PathVariable UUID projectId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal User currentUser) {
        return projectFinanceService.deleteExpense(projectId, expenseId, currentUser);
    }
}
