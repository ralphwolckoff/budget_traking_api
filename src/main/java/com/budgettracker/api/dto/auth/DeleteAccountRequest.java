package com.budgettracker.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAccountRequest {

    @NotBlank(message = "Mot de passe requis")
    private String password;
}
