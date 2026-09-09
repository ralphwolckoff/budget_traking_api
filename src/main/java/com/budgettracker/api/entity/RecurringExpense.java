package com.budgettracker.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

/**
 * L'id est fourni par le client (ex: "rec-1234567890"), pas généré côté serveur —
 * évite tout remapping d'ID lors de la synchro (voir storage.ts : le client reste
 * source de vérité, comme documenté dans le schema Prisma d'origine).
 */
@Entity
@Table(name = "recurring_expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringExpense {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer dayOfMonth;

    @Builder.Default
    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private String startMonth; // "2026-03"

    private String endMonth;
    private String lastGeneratedMonth;
    private String notes;

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
