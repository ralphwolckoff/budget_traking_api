package com.budgettracker.api.repository;

import com.budgettracker.api.entity.Investment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends JpaRepository<Investment, String> {

    List<Investment> findByUserId(String userId);

    Optional<Investment> findByIdAndUserId(String id, String userId);
}
