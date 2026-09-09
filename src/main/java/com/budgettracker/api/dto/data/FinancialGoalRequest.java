package com.budgettracker.api.dto.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FinancialGoalRequest {

    @NotBlank(message = "name requis")
    private String name;

    @NotNull(message = "targetAmount requis")
    private Double targetAmount;

    @NotBlank(message = "targetDate requise")
    private String targetDate;

    @NotBlank(message = "startDate requise")
    private String startDate;

    private List<Object> linkedInvestmentIds;
    private String notes;
    private String status;
}
