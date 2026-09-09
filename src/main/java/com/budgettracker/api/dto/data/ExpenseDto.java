package com.budgettracker.api.dto.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExpenseDto {
    private String id;
    private int amount;
    private String description;
    private String category;
    private String date; // ISO-8601
}
