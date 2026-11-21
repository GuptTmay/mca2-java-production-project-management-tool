package com.taskflow.expense;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.expense.dto.UpdateProjectBudgetRequest;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.project.ProjectRepository;
import com.taskflow.project.ProjectStatus;
import com.taskflow.team.Team;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectFinanceServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectExpenseRepository expenseRepository;
    @Mock
    private ProjectAccessGuard projectAccessGuard;

    private ProjectFinanceService service;

    @BeforeEach
    void setUp() {
        service = new ProjectFinanceService(projectRepository, expenseRepository, projectAccessGuard);
    }

    @Test
    void getFinanceReturnsBudgetAndExpenseTotalsForContentManagers() {
        User contentManager = user(Role.CONTENT_MANAGER);
        Project project = project(contentManager);
        project.setBudgetAmount(new BigDecimal("1250.00"));
        ProjectExpense expense = new ProjectExpense(
                project, "Location rental", "Production", new BigDecimal("300.00"),
                java.time.LocalDate.of(2026, 11, 1), contentManager);

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(expenseRepository.findByProjectIdOrderBySpentOnDescCreatedAtDesc(project.getId()))
                .thenReturn(List.of(expense));

        var response = service.getFinance(project.getId(), contentManager);

        assertThat(response.budgetAmount()).isEqualByComparingTo("1250.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("300.00");
        assertThat(response.remainingBudget()).isEqualByComparingTo("950.00");
        assertThat(response.expenses()).hasSize(1);
    }

    @Test
    void teamMembersCannotViewProjectFinances() {
        User teamMember = user(Role.TEAM_MEMBER);
        Project project = project(teamMember);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThrows(ForbiddenOperationException.class, () -> service.getFinance(project.getId(), teamMember));
        verify(expenseRepository, never()).findByProjectIdOrderBySpentOnDescCreatedAtDesc(project.getId());
    }

    @Test
    void updateBudgetPersistsAmountAndCurrency() {
        User producer = user(Role.PRODUCER);
        Project project = project(producer);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(expenseRepository.findByProjectIdOrderBySpentOnDescCreatedAtDesc(project.getId())).thenReturn(List.of());

        var response = service.updateBudget(
                project.getId(), new UpdateProjectBudgetRequest(new BigDecimal("5000.00"), "INR"), producer);

        assertThat(project.getBudgetAmount()).isEqualByComparingTo("5000.00");
        assertThat(project.getCurrencyCode()).isEqualTo("INR");
        assertThat(response.remainingBudget()).isEqualByComparingTo("5000.00");
    }

    @Test
    void updateBudgetRejectsCurrencyChangeAfterExpensesExist() {
        User producer = user(Role.PRODUCER);
        Project project = project(producer);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(expenseRepository.existsByProjectId(project.getId())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.updateBudget(
                project.getId(), new UpdateProjectBudgetRequest(new BigDecimal("5000.00"), "INR"), producer));
    }

    private static Project project(User user) {
        Team team = new Team("Production", "Media team", user);
        setId(team, UUID.randomUUID());
        Project project = new Project(
                team, "Short Film", "SHORT-FILM", "Shoot and edit", ProjectStatus.ACTIVE,
                java.time.LocalDate.of(2026, 11, 1), java.time.LocalDate.of(2026, 12, 1), user);
        setId(project, UUID.randomUUID());
        return project;
    }

    private static User user(Role role) {
        User user = new User(role.name().toLowerCase() + "@taskflow.io", "hash", "Test", "User", role);
        setId(user, UUID.randomUUID());
        return user;
    }

    private static void setId(Object target, UUID id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
