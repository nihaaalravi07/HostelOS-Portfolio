package com.hostelos.dashboard;

import com.hostelos.accommodation.AccommodationService;
import com.hostelos.accommodation.AccommodationService.Occupancy;
import com.hostelos.finance.LedgerService;
import com.hostelos.housekeeping.CleaningService;
import com.hostelos.maintenance.ComplaintService;
import com.hostelos.mess.MealService;
import com.hostelos.mess.MessAnalyticsService;
import com.hostelos.mess.MessAnalyticsService.Analytics;
import com.hostelos.mess.MessOperationsService;
import com.hostelos.mess.MessOperationsService.MealCounts;
import com.hostelos.resident.Resident;
import com.hostelos.resident.ResidentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only composition of figures from other modules. It owns no business rules and no tables
 * (other than announcements): every number here comes from a module service.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {
    public record AdminDashboard(long totalResidents, Occupancy occupancy, long todayExpected, long todayConsumed,
                                 long todayPending, BigDecimal todayRevenue, BigDecimal monthRevenue, Double avgTaste,
                                 Double avgHygiene, BigDecimal outstandingBalances, long pendingCleaning,
                                 long openComplaints, Analytics mess) {}

    public record WardenDashboard(long totalResidents, Occupancy occupancy, long pendingCleaning, long openComplaints) {}

    private final ResidentService residents;
    private final AccommodationService accommodation;
    private final MealService meals;
    private final MessOperationsService messOps;
    private final MessAnalyticsService messAnalytics;
    private final LedgerService ledger;
    private final CleaningService cleaning;
    private final ComplaintService complaints;
    private final AnnouncementRepository announcements;

    public DashboardService(ResidentService residents, AccommodationService accommodation, MealService meals,
                            MessOperationsService messOps, MessAnalyticsService messAnalytics, LedgerService ledger,
                            CleaningService cleaning, ComplaintService complaints, AnnouncementRepository announcements) {
        this.residents = residents;
        this.accommodation = accommodation;
        this.meals = meals;
        this.messOps = messOps;
        this.messAnalytics = messAnalytics;
        this.ledger = ledger;
        this.cleaning = cleaning;
        this.complaints = complaints;
        this.announcements = announcements;
    }

    public StudentDashboard student() {
        Resident resident = residents.requireCurrent();
        Long id = resident.getId();
        YearMonth month = YearMonth.now();
        LocalDate today = LocalDate.now();
        return new StudentDashboard(
                residents.summary(id),
                accommodation.currentFor(id).orElse(null),
                ledger.netSpend(id, LedgerService.MODULE_MESS, month),
                meals.consumedInMonth(id, month),
                meals.upcomingBookedCount(id),
                ledger.summary(id),
                cleaning.describe(cleaning.nextUpcoming(id)),
                complaints.openCountFor(id),
                meals.list(today, today, id),
                recentAnnouncements(5));
    }

    public AdminDashboard admin() {
        LocalDate today = LocalDate.now();
        MealCounts todayCounts = messOps.board(today).totals();
        Analytics analytics = messAnalytics.analytics(today.minusDays(13), today);
        YearMonth month = YearMonth.now();
        BigDecimal monthRevenue = messAnalytics.analytics(month.atDay(1), today).totals().revenue();
        return new AdminDashboard(residents.countActive(), accommodation.occupancy(), todayCounts.expected(),
                todayCounts.consumed(), todayCounts.pending(), todayCounts.revenue(), monthRevenue,
                analytics.totals().avgTaste(), analytics.totals().avgHygiene(), ledger.totalOutstanding(),
                cleaning.pendingCount(), complaints.openCount(), analytics);
    }

    public WardenDashboard warden() {
        return new WardenDashboard(residents.countActive(), accommodation.occupancy(), cleaning.pendingCount(), complaints.openCount());
    }

    public List<StudentDashboard.AnnouncementDto> recentAnnouncements(int limit) {
        return announcements.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(a -> new StudentDashboard.AnnouncementDto(a.getId(), a.getTitle(), a.getBody(), a.getCreatedAt())).toList();
    }

    @Transactional
    public StudentDashboard.AnnouncementDto announce(String title, String body, Long authorId) {
        Announcement a = announcements.save(new Announcement(title.trim(), body.trim(), authorId, java.time.LocalDateTime.now()));
        return new StudentDashboard.AnnouncementDto(a.getId(), a.getTitle(), a.getBody(), a.getCreatedAt());
    }
}
