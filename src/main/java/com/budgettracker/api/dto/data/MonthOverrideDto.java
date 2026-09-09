package com.budgettracker.api.dto.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthOverrideDto {
    private Integer salary;
    private Integer savings;
}
