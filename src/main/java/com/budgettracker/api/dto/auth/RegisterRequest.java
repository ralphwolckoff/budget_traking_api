package com.budgettracker.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Nom d'utilisateur requis")
    private String username;

    @NotBlank(message = "Mot de passe requis")
    private String password;
}
