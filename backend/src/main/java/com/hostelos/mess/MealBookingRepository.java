package com.hostelos.mess;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealBookingRepository extends JpaRepository<MealBooking, Long> {
    Optional<MealBooking> findByMealIdAndResidentId(Long mealId, Long residentId);

    List<MealBooking> findByMealIdInAndResidentId(Collection<Long> mealIds, Long residentId);

    List<MealBooking> findByMealIdOrderByBookedAtAsc(Long mealId);

    List<MealBooking> findByResidentIdOrderByBookedAtDesc(Long residentId, Pageable page);

    /** Row lock: two staff members confirming the same booking at once are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from MealBooking b where b.id = :id")
    Optional<MealBooking> findByIdForUpdate(@Param("id") Long id);

    @Query("select b from MealBooking b where b.status = 'BOOKED' and b.meal.mealDate < :today")
    List<MealBooking> findStaleBookings(@Param("today") LocalDate today);

    long countByResidentIdAndStatusAndMealMealDateBetween(Long residentId, BookingStatus status, LocalDate from, LocalDate to);

    @Query("select b.meal.id, b.status, count(b) from MealBooking b where b.meal.mealDate between :from and :to group by b.meal.id, b.status")
    List<Object[]> countsByMealAndStatus(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select b.meal.id, b.status, count(b) from MealBooking b where b.meal.id in :ids group by b.meal.id, b.status")
    List<Object[]> countsForMeals(@Param("ids") Collection<Long> ids);

    @Query("select b.meal.id, coalesce(sum(b.chargedAmount), 0) from MealBooking b where b.status = 'CONSUMED' and b.meal.mealDate between :from and :to group by b.meal.id")
    List<Object[]> revenueByMeal(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select coalesce(sum(b.chargedAmount), 0) from MealBooking b where b.status = 'CONSUMED' and b.meal.mealDate between :from and :to")
    java.math.BigDecimal revenue(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
