package com.budgettracker.api.dto.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecurringExpenseDto {
    private String id;
    private String description;
    private String category;
    private int amount;
    private int dayOfMonth;
    private boolean active;
    private String startMonth;
    private String endMonth;
    private String lastGeneratedMonth;
    private String notes;
    private String createdAt;
}
