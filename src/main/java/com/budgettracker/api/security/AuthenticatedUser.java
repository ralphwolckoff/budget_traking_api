package com.budgettracker.api.security;

/**
 * Équivalent du "req.user" injecté par le middleware authenticate() côté Node.
 * Porté par le principal Spring Security une fois le JWT validé.
 */
public record AuthenticatedUser(String userId, String username) {
}
