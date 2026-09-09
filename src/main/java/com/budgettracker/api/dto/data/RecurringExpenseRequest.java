package com.budgettracker.api.dto.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecurringExpenseRequest {

    @NotBlank(message = "description requise")
    private String description;

    @NotBlank(message = "category requise")
    private String category;

    @NotNull(message = "amount requis")
    private Double amount;

    @NotNull(message = "dayOfMonth requis")
    private Integer dayOfMonth;

    private Boolean active;

    @NotBlank(message = "startMonth requis")
    private String startMonth;

    private String endMonth;
    private String lastGeneratedMonth;
    private String notes;
}
