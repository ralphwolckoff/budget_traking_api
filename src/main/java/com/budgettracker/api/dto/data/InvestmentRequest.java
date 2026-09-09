package com.budgettracker.api.dto.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InvestmentRequest {

    @NotBlank(message = "type requis")
    private String type;

    @NotBlank(message = "name requis")
    private String name;

    @NotNull(message = "amount requis")
    private Double amount;

    @NotBlank(message = "startDate requise")
    private String startDate;

    private String endDate;
    private Integer durationMonths;
    private Double expectedReturn;
    private Integer currentValue;
    private String notes;

    @NotBlank(message = "status requis")
    private String status;

    // Nullable : le front peut omettre ces tableaux (ex. à la création) → défaut []
    private List<Object> payments;
    private List<Object> gains;
    private List<Object> events;
    private List<Object> valueHistory;
    private List<Object> documents;
}
