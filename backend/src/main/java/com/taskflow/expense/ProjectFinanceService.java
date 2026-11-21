package com.taskflow.expense;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.expense.dto.CreateExpenseRequest;
import com.taskflow.expense.dto.ExpenseResponse;
import com.taskflow.expense.dto.ProjectFinanceResponse;
import com.taskflow.expense.dto.UpdateProjectBudgetRequest;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.project.ProjectRepository;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectFinanceService {

    private final ProjectRepository projectRepository;
    private final ProjectExpenseRepository expenseRepository;
    private final ProjectAccessGuard projectAccessGuard;

    public ProjectFinanceService(
            ProjectRepository projectRepository,
            ProjectExpenseRepository expenseRepository,
            ProjectAccessGuard projectAccessGuard) {
        this.projectRepository = projectRepository;
        this.expenseRepository = expenseRepository;
        this.projectAccessGuard = projectAccessGuard;
    }

    @Transactional(readOnly = true)
    public ProjectFinanceResponse getFinance(UUID projectId, User currentUser) {
        Project project = findProject(projectId);
        requireFinanceReadAccess(project, currentUser);
        return toResponse(project, expenseRepository.findByProjectIdOrderBySpentOnDescCreatedAtDesc(projectId));
    }

    @Transactional
    public ProjectFinanceResponse updateBudget(
            UUID projectId, UpdateProjectBudgetRequest request, User currentUser) {
        Project project = findProject(projectId);
        projectAccessGuard.requireManageAccess(project, currentUser);
        validateCurrency(request.currencyCode());
        if (!project.getCurrencyCode().equals(request.currencyCode())
                && expenseRepository.existsByProjectId(projectId)) {
            throw new IllegalArgumentException("Project currency cannot be changed after expenses have been recorded");
        }
        project.setBudgetAmount(request.budgetAmount());
        project.setCurrencyCode(request.currencyCode());
        return getFinance(projectId, currentUser);
    }

    @Transactional
    public ProjectFinanceResponse addExpense(UUID projectId, CreateExpenseRequest request, User currentUser) {
        Project project = findProject(projectId);
        projectAccessGuard.requireManageAccess(project, currentUser);
        ProjectExpense expense = new ProjectExpense(
                project,
                request.description().trim(),
                request.category() == null || request.category().isBlank() ? null : request.category().trim(),
                request.amount(),
                request.spentOn(),
                currentUser);
        expenseRepository.save(expense);
        return getFinance(projectId, currentUser);
    }

    @Transactional
    public ProjectFinanceResponse deleteExpense(UUID projectId, UUID expenseId, User currentUser) {
        Project project = findProject(projectId);
        projectAccessGuard.requireManageAccess(project, currentUser);
        ProjectExpense expense = expenseRepository.findByIdAndProjectId(expenseId, projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project expense", expenseId));
        expenseRepository.delete(expense);
        return getFinance(projectId, currentUser);
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", projectId));
    }

    private void requireFinanceReadAccess(Project project, User currentUser) {
        projectAccessGuard.requireViewAccess(project, currentUser);
        if (currentUser.getRole() != Role.PRODUCER && currentUser.getRole() != Role.CONTENT_MANAGER) {
            throw new ForbiddenOperationException("Only Producers and Content Managers can view project finances");
        }
    }

    private void validateCurrency(String currencyCode) {
        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("currencyCode must be a supported ISO 4217 currency");
        }
    }

    private ProjectFinanceResponse toResponse(Project project, List<ProjectExpense> expenses) {
        List<ExpenseResponse> expenseResponses = expenses.stream()
                .map(this::toResponse)
                .toList();
        BigDecimal total = expenseResponses.stream()
                .map(ExpenseResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProjectFinanceResponse(
                project.getId(),
                project.getBudgetAmount(),
                project.getCurrencyCode(),
                total,
                project.getBudgetAmount().subtract(total),
                expenseResponses);
    }

    private ExpenseResponse toResponse(ProjectExpense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getProject().getId(),
                expense.getDescription(),
                expense.getCategory(),
                expense.getAmount(),
                expense.getSpentOn(),
                expense.getCreatedBy().getId(),
                expense.getCreatedAt());
    }
}
