package com.budgettracker.api.repository;

import com.budgettracker.api.entity.MonthSettings;
import com.budgettracker.api.entity.MonthSettingsId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthSettingsRepository extends JpaRepository<MonthSettings, MonthSettingsId> {

    // "findByIdUserId" parcourt la clé embarquée id.userId — équivalent de
    // prisma.monthSettings.findMany({ where: { userId } })
    List<MonthSettings> findByIdUserId(String userId);
}
