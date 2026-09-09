package com.budgettracker.api.dto;

public record ApiErrorResponse(boolean success, String error) {
    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse(false, message);
    }
}
