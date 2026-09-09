package com.budgettracker.api.controller;

import com.budgettracker.api.dto.SuccessResponse;
import com.budgettracker.api.dto.data.*;
import com.budgettracker.api.security.AuthenticatedUser;
import com.budgettracker.api.service.BudgetDataService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Portage direct de routes/data.js. Toutes les routes sont déjà protégées globalement
 * par SecurityConfig (.anyRequest().authenticated()) — pas besoin de middleware ici,
 * @AuthenticationPrincipal AuthenticatedUser récupère l'utilisateur posé par JwtAuthFilter.
 */
@RestController
@RequestMapping("/api/data")
public class DataController {

    private final BudgetDataService budgetDataService;

    public DataController(BudgetDataService budgetDataService) {
        this.budgetDataService = budgetDataService;
    }

    // GET /api/data
    @GetMapping
    public ResponseEntity<Map<String, Object>> getData(@AuthenticationPrincipal AuthenticatedUser user) {
        AppDataResponse data = budgetDataService.buildAppData(user.userId());
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // PUT /api/data/settings — salaire/épargne globaux
    @PutMapping("/settings")
    public ResponseEntity<SuccessResponse> updateSettings(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @Valid @RequestBody SettingsRequest request) {
        budgetDataService.updateSettings(user.userId(), request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // PUT /api/data/month-settings — override salaire/épargne pour un mois donné
    @PutMapping("/month-settings")
    public ResponseEntity<SuccessResponse> updateMonthSettings(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @Valid @RequestBody MonthSettingsRequest request) {
        budgetDataService.updateMonthSettings(user.userId(), request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // POST /api/data/expenses
    @PostMapping("/expenses")
    public ResponseEntity<Map<String, Object>> createExpense(@AuthenticationPrincipal AuthenticatedUser user,
                                                               @Valid @RequestBody ExpenseRequest request) {
        ExpenseDto expense = budgetDataService.createExpense(user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "expense", expense));
    }

    // DELETE /api/data/expenses/{id}
    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<SuccessResponse> deleteExpense(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable String id) {
        budgetDataService.deleteExpense(user.userId(), id);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // POST /api/data/forecast — create si pas d'id, update sinon
    @PostMapping("/forecast")
    public ResponseEntity<Map<String, Object>> upsertForecast(@AuthenticationPrincipal AuthenticatedUser user,
                                                                @Valid @RequestBody ForecastRequest request) {
        ForecastItemDto item = budgetDataService.upsertForecast(user.userId(), request);
        return ResponseEntity.ok(Map.of("success", true, "item", item));
    }

    // DELETE /api/data/forecast/{id}
    @DeleteMapping("/forecast/{id}")
    public ResponseEntity<SuccessResponse> deleteForecast(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @PathVariable String id) {
        budgetDataService.deleteForecast(user.userId(), id);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // PUT /api/data/carryover
    @PutMapping("/carryover")
    public ResponseEntity<SuccessResponse> upsertCarryOver(@AuthenticationPrincipal AuthenticatedUser user,
                                                             @Valid @RequestBody CarryOverRequest request) {
        budgetDataService.upsertCarryOver(user.userId(), request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // PUT /api/data/category-budgets — plafonds par catégorie (objet complet)
    @PutMapping("/category-budgets")
    public ResponseEntity<SuccessResponse> updateCategoryBudgets(@AuthenticationPrincipal AuthenticatedUser user,
                                                                   @Valid @RequestBody CategoryBudgetsRequest request) {
        budgetDataService.updateCategoryBudgets(user.userId(), request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // PUT /api/data/recurring/{id} — upsert whole-object, id fourni par le client
    @PutMapping("/recurring/{id}")
    public ResponseEntity<SuccessResponse> saveRecurring(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable String id,
                                                           @Valid @RequestBody RecurringExpenseRequest request) {
        budgetDataService.saveRecurring(user.userId(), id, request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // DELETE /api/data/recurring/{id}
    @DeleteMapping("/recurring/{id}")
    public ResponseEntity<SuccessResponse> deleteRecurring(@AuthenticationPrincipal AuthenticatedUser user,
                                                             @PathVariable String id) {
        budgetDataService.deleteRecurring(user.userId(), id);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // PUT /api/data/investments/{id} — upsert whole-object, id fourni par le client
    @PutMapping("/investments/{id}")
    public ResponseEntity<SuccessResponse> saveInvestment(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @PathVariable String id,
                                                            @Valid @RequestBody InvestmentRequest request) {
        budgetDataService.saveInvestment(user.userId(), id, request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // DELETE /api/data/investments/{id}
    @DeleteMapping("/investments/{id}")
    public ResponseEntity<SuccessResponse> deleteInvestment(@AuthenticationPrincipal AuthenticatedUser user,
                                                              @PathVariable String id) {
        budgetDataService.deleteInvestment(user.userId(), id);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // PUT /api/data/goals/{id} — upsert whole-object, id fourni par le client
    @PutMapping("/goals/{id}")
    public ResponseEntity<SuccessResponse> saveGoal(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable String id,
                                                      @Valid @RequestBody FinancialGoalRequest request) {
        budgetDataService.saveGoal(user.userId(), id, request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // DELETE /api/data/goals/{id}
    @DeleteMapping("/goals/{id}")
    public ResponseEntity<SuccessResponse> deleteGoal(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @PathVariable String id) {
        budgetDataService.deleteGoal(user.userId(), id);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    // GET /api/data/search?q=Orange Money&year=2026&month=2026-08&category=transport
    // Tous les paramètres sont optionnels ; q vide renvoie toutes les dépenses filtrées
    // par year/month/category le cas échéant. Recherche insensible à la casse sur
    // description ET category.
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchExpenses(@AuthenticationPrincipal AuthenticatedUser user,
                                                                @RequestParam(required = false) String q,
                                                                @RequestParam(required = false) String year,
                                                                @RequestParam(required = false) String month,
                                                                @RequestParam(required = false) String category) {
        var results = budgetDataService.searchExpenses(user.userId(), q, year, month, category);
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }
}
