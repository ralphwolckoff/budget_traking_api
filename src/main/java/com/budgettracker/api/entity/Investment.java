package com.budgettracker.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

/**
 * Comme RecurringExpense, l'id est fourni par le client. Les sous-collections
 * (payments, gains, events, valueHistory, documents) sont stockées en JSON brut
 * (colonnes TEXT) — même logique que le champ Json de Prisma : le client lit/écrit
 * toujours l'investissement complet comme un objet, jamais champ par champ, donc un
 * stockage relationnel normalisé n'apporterait rien ici. La (dé)sérialisation se fait
 * dans BudgetDataService via ObjectMapper.
 *
 * @ColumnDefault sur les colonnes NOT NULL : permet à Hibernate d'ajouter ces colonnes
 * à une table déjà peuplée sans que PostgreSQL ne rejette l'ALTER TABLE.
 */
@Entity
@Table(name = "investments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investment {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private String startDate; // "YYYY-MM-DD"

    private String endDate;
    private Integer durationMonths;
    private Double expectedReturn;
    private Integer currentValue;
    private String notes;

    @Builder.Default
    @ColumnDefault("'actif'")
    @Column(nullable = false)
    private String status = "actif";

    @Builder.Default
    @ColumnDefault("'[]'")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payments = "[]";

    @Builder.Default
    @ColumnDefault("'[]'")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String gains = "[]";

    @Builder.Default
    @ColumnDefault("'[]'")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String events = "[]";

    @Builder.Default
    @ColumnDefault("'[]'")
    @Column(name = "value_history", columnDefinition = "TEXT", nullable = false)
    private String valueHistory = "[]";

    @Builder.Default
    @ColumnDefault("'[]'")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String documents = "[]";

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
