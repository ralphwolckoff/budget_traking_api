package com.budgettracker.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {

    // Relation 1-1 avec User, clé partagée (équivalent du "userId @unique" côté Prisma)
    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @ColumnDefault("150000")
    @Column(nullable = false)
    private Integer salary = 150000;

    @Builder.Default
    @ColumnDefault("30000")
    @Column(nullable = false)
    private Integer savings = 30000;

    // Stocké en JSON brut (comme Investment) — le client lit/écrit toujours l'objet
    // complet { catId: montant, ... }, jamais champ par champ.
    // @ColumnDefault : permet à Hibernate d'ajouter cette colonne à une table déjà
    // peuplée (sinon PostgreSQL refuse un ALTER TABLE ... NOT NULL sans défaut).
    @Builder.Default
    @ColumnDefault("'{}'")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String categoryBudgets = "{}";

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
