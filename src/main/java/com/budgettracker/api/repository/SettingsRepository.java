package com.budgettracker.api.repository;

import com.budgettracker.api.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, String> {
}
