package com.budgettracker.api.dto.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CarryOverRequest {

    @NotBlank(message = "monthKey requis")
    private String monthKey;

    @NotNull(message = "amount requis")
    private Double amount;
}
