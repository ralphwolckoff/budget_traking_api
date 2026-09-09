package com.budgettracker.api.exception;

import com.budgettracker.api.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Équivalent du error handler global en fin de index.js
 * (app.use((err, req, res, next) => { ... })), + des try/catch répétés dans chaque route.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(ApiErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Requête invalide");
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception e) {
        log.error("[unhandled]", e);
        return ResponseEntity.internalServerError().body(ApiErrorResponse.of("Erreur serveur"));
    }
}
