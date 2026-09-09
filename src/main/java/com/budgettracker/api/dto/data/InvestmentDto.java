package com.budgettracker.api.dto.data;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InvestmentDto {
    private String id;
    private String type;
    private String name;
    private int amount;
    private String startDate;
    private String endDate;
    private Integer durationMonths;
    private Double expectedReturn;
    private Integer currentValue;
    private String notes;
    private String status;
    private List<Object> payments;
    private List<Object> gains;
    private List<Object> events;
    private List<Object> valueHistory;
    private List<Object> documents;
}
