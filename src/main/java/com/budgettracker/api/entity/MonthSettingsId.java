package com.budgettracker.api.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

/**
 * Équivalent de la contrainte @@unique([userId, monthKey]) / userId_monthKey
 * utilisée par Prisma pour MonthSettings.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MonthSettingsId implements Serializable {
    private String userId;
    private String monthKey;
}
