package com.budgettracker.api.dto.data;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SettingsRequest {

    @NotNull(message = "salary requis")
    private Double salary;

    @NotNull(message = "savings requis")
    private Double savings;
}
