package com.budgettracker.api.dto.data;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForecastRequest {

    // Présent = update, absent/null = create (identique à la logique `if (id)` côté Node)
    private String id;

    private String monthKey;
    private String catId;
    private String label;

    @NotNull(message = "price requis")
    private Double price;

    private Boolean done;
}
