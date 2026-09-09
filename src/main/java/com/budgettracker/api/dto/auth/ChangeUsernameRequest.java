package com.budgettracker.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeUsernameRequest {

    @NotBlank(message = "Mot de passe requis")
    private String password;

    @NotBlank(message = "Nouveau nom requis")
    private String newUsername;
}
