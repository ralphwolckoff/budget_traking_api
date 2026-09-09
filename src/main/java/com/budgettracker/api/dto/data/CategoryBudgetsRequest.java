package com.budgettracker.api.dto.data;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CategoryBudgetsRequest {

    @NotNull(message = "categoryBudgets requis")
    private Map<String, Object> categoryBudgets;
}
