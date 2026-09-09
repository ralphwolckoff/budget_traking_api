package com.budgettracker.api.repository;

import com.budgettracker.api.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, String> {

    List<RecurringExpense> findByUserId(String userId);

    Optional<RecurringExpense> findByIdAndUserId(String id, String userId);
}
