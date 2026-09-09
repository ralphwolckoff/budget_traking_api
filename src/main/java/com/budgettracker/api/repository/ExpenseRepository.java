package com.budgettracker.api.repository;

import com.budgettracker.api.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, String> {

    List<Expense> findByUserIdOrderByDateDesc(String userId);

    // Combine la recherche et le contrôle IDOR (expense.userId !== req.user.userId côté Node)
    // en une seule requête.
    Optional<Expense> findByIdAndUserId(String id, String userId);

    // Pour les rapports : toutes les dépenses d'un mois donné, triées chronologiquement
    List<Expense> findByUserIdAndMonthKeyOrderByDateAsc(String userId, String monthKey);

    // Pour la recherche globale : correspond sur la description OU la catégorie,
    // insensible à la casse — le filtrage par année/mois/catégorie se fait ensuite en
    // mémoire dans le service (volumes de données personnelles, pas besoin d'optimiser ici).
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId AND "
            + "(LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(e.category) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "ORDER BY e.date DESC")
    List<Expense> searchByUserIdAndText(@Param("userId") String userId, @Param("q") String q);
}
