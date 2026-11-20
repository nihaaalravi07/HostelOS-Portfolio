package com.hostelos.mess;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodFeedbackRepository extends JpaRepository<FoodFeedback, Long> {
    Optional<FoodFeedback> findByMealIdAndResidentId(Long mealId, Long residentId);

    List<FoodFeedback> findByMealIdInAndResidentId(Collection<Long> mealIds, Long residentId);

    List<FoodFeedback> findAllByOrderByCreatedAtDesc(Pageable page);

    @Query("select f.meal.id, avg(f.tasteRating), avg(f.hygieneRating), count(f) from FoodFeedback f where f.meal.id in :ids group by f.meal.id")
    List<Object[]> averagesForMeals(@Param("ids") Collection<Long> ids);

    @Query("select f from FoodFeedback f where f.meal.mealDate between :from and :to")
    List<FoodFeedback> inRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
