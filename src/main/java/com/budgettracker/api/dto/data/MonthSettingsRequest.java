package com.budgettracker.api.dto.data;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MonthSettingsRequest {

    @NotBlank(message = "monthKey requis")
    private String monthKey;

    // Nullable volontairement : null = pas d'override / suppression (comme côté Node)
    private Double salary;
    private Double savings;
}
