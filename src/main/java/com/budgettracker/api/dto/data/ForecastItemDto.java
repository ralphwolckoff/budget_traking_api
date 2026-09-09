package com.budgettracker.api.dto.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForecastItemDto {
    private String id;
    private String catId;
    private String label;
    private int price;
    private boolean done;
}
