package com.budgettracker.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Mot de passe actuel requis")
    private String oldPassword;

    @NotBlank(message = "Nouveau mot de passe requis")
    @Size(min = 4, message = "Nouveau mot de passe trop court")
    private String newPassword;
}
