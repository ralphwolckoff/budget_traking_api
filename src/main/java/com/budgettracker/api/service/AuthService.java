package com.budgettracker.api.service;

import com.budgettracker.api.dto.auth.*;
import com.budgettracker.api.entity.Settings;
import com.budgettracker.api.entity.User;
import com.budgettracker.api.exception.ApiException;
import com.budgettracker.api.repository.UserRepository;
import com.budgettracker.api.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Portage direct de la logique des 5 routes de routes/auth.js.
 * Contrairement au Node original, la logique métier est ici extraite des controllers.
 */
@Service
public class AuthService {

    // Équivalent de sanitize() dans routes/auth.js
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-z0-9_-]");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    private String sanitize(String raw) {
        if (raw == null) return "";
        String lower = raw.toLowerCase();
        String cleaned = INVALID_CHARS.matcher(lower).replaceAll("");
        return cleaned.length() > 32 ? cleaned.substring(0, 32) : cleaned;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = sanitize(request.getUsername());
        if (username.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nom trop court (min 2 car.)");
        }
        if (request.getPassword() == null || request.getPassword().length() < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mot de passe trop court (min 4 car.)");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce nom d'utilisateur est déjà pris");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        Settings settings = Settings.builder()
                .user(user)
                .salary(150000)
                .savings(30000)
                .build();
        user.setSettings(settings);

        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), username);
        return AuthResponse.builder().success(true).username(username).token(token).build();
    }

    public AuthResponse login(LoginRequest request) {
        String username = sanitize(request.getUsername());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects");
        }

        String token = jwtService.generateToken(user.getId(), username);
        return AuthResponse.builder().success(true).username(username).token(token).build();
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Mot de passe actuel incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse changeUsername(String userId, ChangeUsernameRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect");
        }

        String newUsername = sanitize(request.getNewUsername());
        if (newUsername.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nouveau nom trop court");
        }
        if (userRepository.existsByUsername(newUsername)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce nom est déjà pris");
        }

        user.setUsername(newUsername);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), newUsername);
        return AuthResponse.builder().success(true).username(newUsername).token(token).build();
    }

    @Transactional
    public void deleteAccount(String userId, DeleteAccountRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Mot de passe incorrect");
        }

        userRepository.delete(user);
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }
}
