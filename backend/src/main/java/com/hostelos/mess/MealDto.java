package com.hostelos.mess;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** A meal as the API presents it, optionally enriched with the signed-in resident's own status. */
public record MealDto(Long id, LocalDate date, MealCategory category, String categoryLabel, LocalTime startTime,
                      LocalTime endTime, List<String> menuItems, BigDecimal price, LocalDateTime cutoffTime,
                      boolean bookingOpen, boolean available, String imageUrl,
                      BookingStatus myStatus, Long myBookingId, MyFeedback myFeedback,
                      Double avgTaste, Double avgHygiene, long ratingCount) {

    public record MyFeedback(int tasteRating, int hygieneRating, String comment) {}
}
