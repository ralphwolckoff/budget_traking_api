package com.budgettracker.api.dto.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExpenseRequest {

    @NotNull(message = "amount requis")
    private Double amount;

    @NotBlank(message = "description requise")
    private String description;

    @NotBlank(message = "category requise")
    private String category;

    @NotBlank(message = "date requise")
    private String date; // ISO-8601, parsé en Instant côté service

    @NotBlank(message = "monthKey requis")
    private String monthKey;
}
