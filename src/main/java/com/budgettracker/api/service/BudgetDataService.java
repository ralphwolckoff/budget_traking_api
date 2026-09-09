package com.budgettracker.api.service;

import com.budgettracker.api.dto.data.*;
import com.budgettracker.api.entity.*;
import com.budgettracker.api.exception.ApiException;
import com.budgettracker.api.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Portage direct de routes/data.js : buildAppData() + tous les handlers CRUD,
 * y compris recurring/investments (upsert whole-object par id client, comme dans storage.ts).
 */
@Service
public class BudgetDataService {

    private final SettingsRepository settingsRepository;
    private final MonthSettingsRepository monthSettingsRepository;
    private final ExpenseRepository expenseRepository;
    private final ForecastItemRepository forecastItemRepository;
    private final CarryOverRepository carryOverRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final InvestmentRepository investmentRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public BudgetDataService(SettingsRepository settingsRepository,
                              MonthSettingsRepository monthSettingsRepository,
                              ExpenseRepository expenseRepository,
                              ForecastItemRepository forecastItemRepository,
                              CarryOverRepository carryOverRepository,
                              RecurringExpenseRepository recurringExpenseRepository,
                              InvestmentRepository investmentRepository,
                              FinancialGoalRepository financialGoalRepository,
                              UserRepository userRepository,
                              ObjectMapper objectMapper) {
        this.settingsRepository = settingsRepository;
        this.monthSettingsRepository = monthSettingsRepository;
        this.expenseRepository = expenseRepository;
        this.forecastItemRepository = forecastItemRepository;
        this.carryOverRepository = carryOverRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.investmentRepository = investmentRepository;
        this.financialGoalRepository = financialGoalRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    // ── GET /api/data — équivalent de buildAppData() ──────────────────────────

    public AppDataResponse buildAppData(String userId) {
        Settings settings = settingsRepository.findById(userId).orElse(null);
        List<MonthSettings> monthSettingsList = monthSettingsRepository.findByIdUserId(userId);
        List<Expense> expenses = expenseRepository.findByUserIdOrderByDateDesc(userId);
        List<ForecastItem> forecasts = forecastItemRepository.findByUserId(userId);
        List<CarryOver> carryOvers = carryOverRepository.findByIdUserId(userId);
        List<RecurringExpense> recurringExpenses = recurringExpenseRepository.findByUserId(userId);
        List<Investment> investments = investmentRepository.findByUserId(userId);
        List<FinancialGoal> goals = financialGoalRepository.findByUserId(userId);

        Map<String, List<ExpenseDto>> months = new LinkedHashMap<>();
        for (Expense e : expenses) {
            months.computeIfAbsent(e.getMonthKey(), k -> new ArrayList<>()).add(toDto(e));
        }

        Map<String, List<ForecastItemDto>> forecastItems = new LinkedHashMap<>();
        for (ForecastItem f : forecasts) {
            forecastItems.computeIfAbsent(f.getMonthKey(), k -> new ArrayList<>()).add(toDto(f));
        }

        Map<String, Integer> carryOver = new LinkedHashMap<>();
        for (CarryOver c : carryOvers) {
            carryOver.put(c.getId().getMonthKey(), c.getAmount());
        }

        Map<String, MonthOverrideDto> monthOverrides = new LinkedHashMap<>();
        for (MonthSettings ms : monthSettingsList) {
            monthOverrides.put(ms.getId().getMonthKey(),
                    MonthOverrideDto.builder().salary(ms.getSalary()).savings(ms.getSavings()).build());
        }

        // Indexés par id (pas groupés par mois) — c'est ce que storage.ts attend
        Map<String, RecurringExpenseDto> recurringMap = new LinkedHashMap<>();
        for (RecurringExpense r : recurringExpenses) {
            recurringMap.put(r.getId(), toDto(r));
        }

        Map<String, InvestmentDto> investmentMap = new LinkedHashMap<>();
        for (Investment inv : investments) {
            investmentMap.put(inv.getId(), toDto(inv));
        }

        Map<String, FinancialGoalDto> goalMap = new LinkedHashMap<>();
        for (FinancialGoal g : goals) {
            goalMap.put(g.getId(), toDto(g));
        }

        return AppDataResponse.builder()
                .salary(settings != null ? settings.getSalary() : 150000)
                .savings(settings != null ? settings.getSavings() : 30000)
                .months(months)
                .forecastItems(forecastItems)
                .carryOver(carryOver)
                .monthOverrides(monthOverrides)
                .recurringExpenses(recurringMap)
                .investments(investmentMap)
                .categoryBudgets(fromJsonMap(settings != null ? settings.getCategoryBudgets() : "{}"))
                .goals(goalMap)
                .updatedAt(settings != null ? settings.getUpdatedAt().toString() : Instant.now().toString())
                .build();
    }

    // ── PUT /api/data/settings ─────────────────────────────────────────────────

    @Transactional
    public void updateSettings(String userId, SettingsRequest request) {
        Settings settings = settingsRepository.findById(userId).orElseGet(() -> {
            User userRef = userRepository.getReferenceById(userId);
            return Settings.builder().user(userRef).build();
        });
        settings.setSalary((int) Math.round(request.getSalary()));
        settings.setSavings((int) Math.round(request.getSavings()));
        settingsRepository.save(settings);
    }

    // ── PUT /api/data/month-settings ───────────────────────────────────────────

    @Transactional
    public void updateMonthSettings(String userId, MonthSettingsRequest request) {
        Integer salaryVal = request.getSalary() != null ? (int) Math.round(request.getSalary()) : null;
        Integer savingsVal = request.getSavings() != null ? (int) Math.round(request.getSavings()) : null;

        MonthSettingsId id = new MonthSettingsId(userId, request.getMonthKey());

        if (salaryVal == null && savingsVal == null) {
            if (monthSettingsRepository.existsById(id)) {
                monthSettingsRepository.deleteById(id);
            }
            return;
        }

        MonthSettings ms = monthSettingsRepository.findById(id)
                .orElseGet(() -> MonthSettings.builder().id(id).build());
        ms.setSalary(salaryVal);
        ms.setSavings(savingsVal);
        monthSettingsRepository.save(ms);
    }

    // ── POST /api/data/expenses ─────────────────────────────────────────────────

    @Transactional
    public ExpenseDto createExpense(String userId, ExpenseRequest request) {
        Expense expense = Expense.builder()
                .userId(userId)
                .monthKey(request.getMonthKey())
                .amount((int) Math.round(request.getAmount()))
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .date(Instant.parse(request.getDate()))
                .build();
        expense = expenseRepository.save(expense);
        return toDto(expense);
    }

    // ── DELETE /api/data/expenses/:id ───────────────────────────────────────────

    @Transactional
    public void deleteExpense(String userId, String expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dépense introuvable"));
        expenseRepository.delete(expense);
    }

    // ── POST /api/data/forecast (create ou update selon id) ────────────────────

    @Transactional
    public ForecastItemDto upsertForecast(String userId, ForecastRequest request) {
        ForecastItem item;
        if (request.getId() != null && !request.getId().isBlank()) {
            item = forecastItemRepository.findByIdAndUserId(request.getId(), userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prévision introuvable"));
            item.setCatId(request.getCatId());
            item.setLabel(request.getLabel());
            item.setPrice((int) Math.round(request.getPrice()));
            item.setDone(Boolean.TRUE.equals(request.getDone()));
        } else {
            item = ForecastItem.builder()
                    .userId(userId)
                    .monthKey(request.getMonthKey())
                    .catId(request.getCatId())
                    .label(request.getLabel())
                    .price((int) Math.round(request.getPrice()))
                    .done(false)
                    .build();
        }
        item = forecastItemRepository.save(item);
        return toDto(item);
    }

    // ── DELETE /api/data/forecast/:id ───────────────────────────────────────────

    @Transactional
    public void deleteForecast(String userId, String id) {
        ForecastItem item = forecastItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prévision introuvable"));
        forecastItemRepository.delete(item);
    }

    // ── PUT /api/data/carryover ──────────────────────────────────────────────────

    @Transactional
    public void upsertCarryOver(String userId, CarryOverRequest request) {
        CarryOverId id = new CarryOverId(userId, request.getMonthKey());
        CarryOver carryOver = carryOverRepository.findById(id)
                .orElseGet(() -> CarryOver.builder().id(id).build());
        carryOver.setAmount((int) Math.round(request.getAmount()));
        carryOverRepository.save(carryOver);
    }

    // ── PUT /api/data/recurring/:id — upsert whole-object, id fourni par le client ─

    @Transactional
    public void saveRecurring(String userId, String id, RecurringExpenseRequest request) {
        // Si l'id existe déjà mais appartient à un autre user → 404 (pas 403, pour ne pas
        // révéler l'existence de la ressource — même logique que expense/forecast)
        Optional<RecurringExpense> existingAnyOwner = recurringExpenseRepository.findById(id);
        if (existingAnyOwner.isPresent() && !existingAnyOwner.get().getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Dépense récurrente introuvable");
        }

        RecurringExpense entity = existingAnyOwner.orElseGet(() ->
                RecurringExpense.builder().id(id).userId(userId).build());

        entity.setDescription(request.getDescription());
        entity.setCategory(request.getCategory());
        entity.setAmount((int) Math.round(request.getAmount()));
        entity.setDayOfMonth(request.getDayOfMonth());
        entity.setActive(request.getActive() == null || request.getActive());
        entity.setStartMonth(request.getStartMonth());
        entity.setEndMonth(request.getEndMonth());
        entity.setLastGeneratedMonth(request.getLastGeneratedMonth());
        entity.setNotes(request.getNotes());

        recurringExpenseRepository.save(entity);
    }

    // ── DELETE /api/data/recurring/:id ──────────────────────────────────────────

    @Transactional
    public void deleteRecurring(String userId, String id) {
        RecurringExpense entity = recurringExpenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dépense récurrente introuvable"));
        recurringExpenseRepository.delete(entity);
    }

    // ── PUT /api/data/investments/:id — upsert whole-object, id fourni par le client ─

    @Transactional
    public void saveInvestment(String userId, String id, InvestmentRequest request) {
        Optional<Investment> existingAnyOwner = investmentRepository.findById(id);
        if (existingAnyOwner.isPresent() && !existingAnyOwner.get().getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Investissement introuvable");
        }

        Investment entity = existingAnyOwner.orElseGet(() ->
                Investment.builder().id(id).userId(userId).build());

        entity.setType(request.getType());
        entity.setName(request.getName());
        entity.setAmount((int) Math.round(request.getAmount()));
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setDurationMonths(request.getDurationMonths());
        entity.setExpectedReturn(request.getExpectedReturn());
        entity.setCurrentValue(request.getCurrentValue());
        entity.setNotes(request.getNotes());
        entity.setStatus(request.getStatus());
        entity.setPayments(toJson(request.getPayments()));
        entity.setGains(toJson(request.getGains()));
        entity.setEvents(toJson(request.getEvents()));
        entity.setValueHistory(toJson(request.getValueHistory()));
        entity.setDocuments(toJson(request.getDocuments()));

        investmentRepository.save(entity);
    }

    // ── DELETE /api/data/investments/:id ────────────────────────────────────────

    @Transactional
    public void deleteInvestment(String userId, String id) {
        Investment entity = investmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Investissement introuvable"));
        investmentRepository.delete(entity);
    }

    // ── GET /api/data/search — recherche globale à travers tous les mois ───────

    public List<ExpenseSearchResultDto> searchExpenses(String userId, String q, String year,
                                                         String month, String category) {
        String query = (q != null) ? q.trim() : "";
        List<Expense> results = query.isEmpty()
                ? expenseRepository.findByUserIdOrderByDateDesc(userId)
                : expenseRepository.searchByUserIdAndText(userId, query);

        return results.stream()
                .filter(e -> year == null || e.getMonthKey().startsWith(year))
                .filter(e -> month == null || e.getMonthKey().equals(month))
                .filter(e -> category == null || e.getCategory().equalsIgnoreCase(category))
                .map(this::toSearchDto)
                .toList();
    }

    // ── PUT /api/data/category-budgets ──────────────────────────────────────────

    @Transactional
    public void updateCategoryBudgets(String userId, CategoryBudgetsRequest request) {
        Settings settings = settingsRepository.findById(userId).orElseGet(() -> {
            User userRef = userRepository.getReferenceById(userId);
            return Settings.builder().user(userRef).build();
        });
        settings.setCategoryBudgets(toJson(request.getCategoryBudgets()));
        settingsRepository.save(settings);
    }

    // ── PUT /api/data/goals/:id — upsert whole-object, id fourni par le client ──

    @Transactional
    public void saveGoal(String userId, String id, FinancialGoalRequest request) {
        Optional<FinancialGoal> existingAnyOwner = financialGoalRepository.findById(id);
        if (existingAnyOwner.isPresent() && !existingAnyOwner.get().getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Objectif introuvable");
        }

        FinancialGoal entity = existingAnyOwner.orElseGet(() ->
                FinancialGoal.builder().id(id).userId(userId).build());

        entity.setName(request.getName());
        entity.setTargetAmount((int) Math.round(request.getTargetAmount()));
        entity.setTargetDate(request.getTargetDate());
        entity.setStartDate(request.getStartDate());
        entity.setLinkedInvestmentIds(toJson(request.getLinkedInvestmentIds()));
        entity.setNotes(request.getNotes());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : "actif");

        financialGoalRepository.save(entity);
    }

    // ── DELETE /api/data/goals/:id ───────────────────────────────────────────────

    @Transactional
    public void deleteGoal(String userId, String id) {
        FinancialGoal entity = financialGoalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Objectif introuvable"));
        financialGoalRepository.delete(entity);
    }

    // ── Mappers ──────────────────────────────────────────────────────────────────

    private ExpenseDto toDto(Expense e) {
        return ExpenseDto.builder()
                .id(e.getId())
                .amount(e.getAmount())
                .description(e.getDescription())
                .category(e.getCategory())
                .date(e.getDate().toString())
                .build();
    }

    private ForecastItemDto toDto(ForecastItem f) {
        return ForecastItemDto.builder()
                .id(f.getId())
                .catId(f.getCatId())
                .label(f.getLabel())
                .price(f.getPrice())
                .done(f.isDone())
                .build();
    }

    private RecurringExpenseDto toDto(RecurringExpense r) {
        return RecurringExpenseDto.builder()
                .id(r.getId())
                .description(r.getDescription())
                .category(r.getCategory())
                .amount(r.getAmount())
                .dayOfMonth(r.getDayOfMonth())
                .active(r.isActive())
                .startMonth(r.getStartMonth())
                .endMonth(r.getEndMonth())
                .lastGeneratedMonth(r.getLastGeneratedMonth())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt().toString())
                .build();
    }

    private InvestmentDto toDto(Investment inv) {
        return InvestmentDto.builder()
                .id(inv.getId())
                .type(inv.getType())
                .name(inv.getName())
                .amount(inv.getAmount())
                .startDate(inv.getStartDate())
                .endDate(inv.getEndDate())
                .durationMonths(inv.getDurationMonths())
                .expectedReturn(inv.getExpectedReturn())
                .currentValue(inv.getCurrentValue())
                .notes(inv.getNotes())
                .status(inv.getStatus())
                .payments(fromJson(inv.getPayments()))
                .gains(fromJson(inv.getGains()))
                .events(fromJson(inv.getEvents()))
                .valueHistory(fromJson(inv.getValueHistory()))
                .documents(fromJson(inv.getDocuments()))
                .build();
    }

    private FinancialGoalDto toDto(FinancialGoal g) {
        return FinancialGoalDto.builder()
                .id(g.getId())
                .name(g.getName())
                .targetAmount(g.getTargetAmount())
                .targetDate(g.getTargetDate())
                .startDate(g.getStartDate())
                .linkedInvestmentIds(fromJson(g.getLinkedInvestmentIds()))
                .notes(g.getNotes())
                .status(g.getStatus())
                .createdAt(g.getCreatedAt().toString())
                .build();
    }

    private ExpenseSearchResultDto toSearchDto(Expense e) {
        return ExpenseSearchResultDto.builder()
                .id(e.getId())
                .monthKey(e.getMonthKey())
                .amount(e.getAmount())
                .description(e.getDescription())
                .category(e.getCategory())
                .date(e.getDate().toString())
                .build();
    }

    // ── JSON helpers (sous-collections Investment) ──────────────────────────────

    private String toJson(List<Object> value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : List.of());
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Format JSON invalide");
        }
    }

    private List<Object> fromJson(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<List<Object>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : Map.of());
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Format JSON invalide");
        }
    }

    private Map<String, Object> fromJsonMap(String raw) {
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
