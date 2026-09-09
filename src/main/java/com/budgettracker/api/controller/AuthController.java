package com.budgettracker.api.controller;

import com.budgettracker.api.dto.SuccessResponse;
import com.budgettracker.api.dto.auth.*;
import com.budgettracker.api.security.AuthenticatedUser;
import com.budgettracker.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Portage direct de routes/auth.js. Les 3 routes protégées (change-password,
 * change-username, delete) n'ont plus besoin d'un middleware "authenticate" explicite :
 * elles sont couvertes par la règle .anyRequest().authenticated() de SecurityConfig,
 * et @AuthenticationPrincipal récupère l'utilisateur posé par JwtAuthFilter.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<SuccessResponse> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.userId(), request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }

    @PostMapping("/change-username")
    public ResponseEntity<AuthResponse> changeUsername(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @Valid @RequestBody ChangeUsernameRequest request) {
        return ResponseEntity.ok(authService.changeUsername(user.userId(), request));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<SuccessResponse> deleteAccount(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @Valid @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(user.userId(), request);
        return ResponseEntity.ok(SuccessResponse.ok());
    }
}
