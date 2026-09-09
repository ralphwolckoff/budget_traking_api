package com.budgettracker.api.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Override salaire/épargne pour un mois spécifique. salary/savings peuvent être null
 * individuellement (revient au global) — équivalent du modèle Prisma MonthSettings.
 */
@Entity
@Table(name = "month_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthSettings {

    @EmbeddedId
    private MonthSettingsId id;

    private Integer salary;
    private Integer savings;
}
