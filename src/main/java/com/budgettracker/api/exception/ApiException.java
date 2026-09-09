package com.budgettracker.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Équivalent des res.status(xxx).json({success:false, error:'...'}) dispersés
 * dans chaque route Node — ici centralisés et levés depuis les services.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
