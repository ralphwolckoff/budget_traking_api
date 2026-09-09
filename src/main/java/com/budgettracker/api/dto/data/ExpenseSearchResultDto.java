package com.budgettracker.api.dto.data;

import lombok.Builder;
import lombok.Data;

/**
 * Comme ExpenseDto, mais avec monthKey en plus — nécessaire ici puisque les résultats
 * de recherche traversent plusieurs mois (contrairement à buildAppData() où les dépenses
 * sont déjà groupées par mois, rendant monthKey redondant dans ExpenseDto).
 */
@Data
@Builder
public class ExpenseSearchResultDto {
    private String id;
    private String monthKey;
    private int amount;
    private String description;
    private String category;
    private String date;
}
