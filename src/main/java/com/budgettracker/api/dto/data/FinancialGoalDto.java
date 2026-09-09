package com.budgettracker.api.dto.data;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FinancialGoalDto {
    private String id;
    private String name;
    private int targetAmount;
    private String targetDate;
    private String startDate;
    private List<Object> linkedInvestmentIds;
    private String notes;
    private String status;
    private String createdAt;
}
