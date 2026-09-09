package com.budgettracker.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

/**
 * Comme RecurringExpense/Investment, l'id est fourni par le client.
 * linkedInvestmentIds est stocké en JSON brut (tableau d'ids), comme les
 * sous-collections d'Investment.
 */
@Entity
@Table(name = "financial_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialGoal {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer targetAmount;

    @Column(nullable = false)
    private String targetDate; // "YYYY-MM-DD"

    @Column(nullable = false)
    private String startDate; // "YYYY-MM-DD"

    @Builder.Default
    @ColumnDefault("'[]'")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String linkedInvestmentIds = "[]";

    private String notes;

    @Builder.Default
    @ColumnDefault("'actif'")
    @Column(nullable = false)
    private String status = "actif";

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
