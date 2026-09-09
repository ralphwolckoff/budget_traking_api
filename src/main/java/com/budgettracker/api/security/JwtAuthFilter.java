package com.budgettracker.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Équivalent de la fonction authenticate() dans middleware/auth.js.
 * Lit "Authorization: Bearer <token>", vérifie le JWT, et peuple le SecurityContext.
 * Le rejet en 401 des routes protégées est délégué à SecurityConfig (authorizeHttpRequests +
 * authenticationEntryPoint), pas à ce filtre.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parseToken(token);
                String userId = claims.get("userId", String.class);
                String username = claims.get("username", String.class);

                AuthenticatedUser principal = new AuthenticatedUser(userId, username);
                var authToken = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (JwtException | IllegalArgumentException e) {
                // Token invalide ou expiré : équivalent du catch{} qui renvoie 401 côté Node.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
