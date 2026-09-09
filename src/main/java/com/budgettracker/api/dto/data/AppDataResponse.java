package com.budgettracker.api.dto.data;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Équivalent exact de l'objet renvoyé par buildAppData() dans routes/data.js.
 * recurringExpenses/investments sont indexés par id (Map<id, dto>), pas groupés par
 * mois — c'est la structure que storage.ts attend (merged.recurringExpenses[a.id] = ...).
 */
@Data
@Builder
public class AppDataResponse {
    private int salary;
    private int savings;
    private Map<String, List<ExpenseDto>> months;
    private Map<String, List<ForecastItemDto>> forecastItems;
    private Map<String, Integer> carryOver;
    private Map<String, MonthOverrideDto> monthOverrides;
    private Map<String, RecurringExpenseDto> recurringExpenses;
    private Map<String, InvestmentDto> investments;
    private Map<String, Object> categoryBudgets;
    private Map<String, FinancialGoalDto> goals;
    private String updatedAt;
}
