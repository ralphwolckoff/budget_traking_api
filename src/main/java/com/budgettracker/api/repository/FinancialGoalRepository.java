package com.budgettracker.api.repository;

import com.budgettracker.api.entity.FinancialGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, String> {

    List<FinancialGoal> findByUserId(String userId);

    Optional<FinancialGoal> findByIdAndUserId(String id, String userId);
}
