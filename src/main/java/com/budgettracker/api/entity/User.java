package com.budgettracker.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    // Équivalent du "hash" Node (bcrypt). Le salt est déjà embarqué dans le hash bcrypt,
    // donc pas besoin d'une colonne "salt" séparée comme dans le schema Prisma d'origine.
    @Column(nullable = false)
    private String passwordHash;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Settings settings;
}
