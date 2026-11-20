package com.hostelos.mess;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findByMealDateBetween(LocalDate from, LocalDate to);
    Optional<Meal> findByMealDateAndCategory(LocalDate date, MealCategory category);
    boolean existsByMealDateAndCategory(LocalDate date, MealCategory category);
}
