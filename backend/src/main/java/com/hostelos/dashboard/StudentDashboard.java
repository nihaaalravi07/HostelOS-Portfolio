package com.hostelos.dashboard;

import com.hostelos.accommodation.AccommodationInfo;
import com.hostelos.finance.LedgerService;
import com.hostelos.housekeeping.CleaningService.CleaningDto;
import com.hostelos.mess.MealDto;
import com.hostelos.resident.ResidentSummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StudentDashboard(ResidentSummary profile, AccommodationInfo accommodation, BigDecimal messSpendThisMonth,
                               long mealsConsumedThisMonth, long upcomingBookedMeals, LedgerService.Summary account,
                               CleaningDto nextCleaning, long openComplaints, List<MealDto> todaysMenu,
                               List<AnnouncementDto> announcements) {

    public record AnnouncementDto(Long id, String title, String body, LocalDateTime createdAt) {}
}
