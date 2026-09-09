package com.budgettracker.api.repository;

import com.budgettracker.api.entity.CarryOver;
import com.budgettracker.api.entity.CarryOverId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarryOverRepository extends JpaRepository<CarryOver, CarryOverId> {

    List<CarryOver> findByIdUserId(String userId);
}
