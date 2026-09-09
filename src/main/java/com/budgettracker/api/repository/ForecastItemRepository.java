package com.budgettracker.api.repository;

import com.budgettracker.api.entity.ForecastItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForecastItemRepository extends JpaRepository<ForecastItem, String> {

    List<ForecastItem> findByUserId(String userId);

    Optional<ForecastItem> findByIdAndUserId(String id, String userId);
}
