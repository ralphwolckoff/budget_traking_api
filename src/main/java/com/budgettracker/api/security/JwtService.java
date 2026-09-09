package com.budgettracker.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Équivalent de signToken()/jwt.verify() dans middleware/auth.js.
 * Différence volontaire : pas de secret par défaut hardcodé — l'appli refuse de démarrer
 * si app.jwt.secret (JWT_SECRET) n'est pas fourni (voir la validation dans le constructeur).
 */
@Service
public class JwtService {

    private static final long EXPIRATION_MS = 30L * 24 * 60 * 60 * 1000; // 30 jours, aligné sur le backend Node

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET manquant. Définis la variable d'environnement JWT_SECRET " +
                "(génère-en une avec : openssl rand -base64 32)."
            );
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
                .claim("userId", userId)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
