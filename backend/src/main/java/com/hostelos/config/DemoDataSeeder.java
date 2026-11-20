package com.hostelos.config;

import com.hostelos.accommodation.AccommodationService;
import com.hostelos.accommodation.AccommodationService.HostelView;
import com.hostelos.dashboard.DashboardService;
import com.hostelos.finance.LedgerService;
import com.hostelos.finance.LedgerService.ChargePosting;
import com.hostelos.housekeeping.CleaningDemoData;
import com.hostelos.maintenance.ComplaintDemoData;
import com.hostelos.mess.MealScheduleService;
import com.hostelos.mess.MessDemoData;
import com.hostelos.mess.MessOperationsService;
import com.hostelos.resident.Resident;
import com.hostelos.resident.ResidentService;
import com.hostelos.user.Role;
import com.hostelos.user.User;
import com.hostelos.user.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Starts the app in a presentable state. Composition root only: it calls module services and holds no business rules.
 * The seed is deterministic (fixed Random seed). Dates are relative to "today" so the demo never looks stale.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    // name, email-local-part, enrolment, course, year, hostel index (0 = Aryabhatta, 1 = Kalpana)
    private static final String[][] STUDENTS = {
        {"Aarav Sharma", "aarav.sharma", "IIT2023001", "B.Tech IT", "3", "0"},
        {"Priya Verma", "priya.verma", "IIT2023002", "B.Tech IT", "3", "1"},
        {"Rohan Gupta", "rohan.gupta", "IIT2023003", "B.Tech IT", "3", "0"},
        {"Ananya Singh", "ananya.singh", "IEC2023004", "B.Tech ECE", "3", "1"},
        {"Vikram Rao", "vikram.rao", "IIT2022005", "B.Tech IT", "4", "0"},
        {"Sneha Iyer", "sneha.iyer", "IEC2022006", "B.Tech ECE", "4", "1"},
        {"Karthik Reddy", "karthik.reddy", "IIT2024007", "B.Tech IT", "2", "0"},
        {"Neha Joshi", "neha.joshi", "IIT2024008", "B.Tech IT", "2", "1"},
        {"Arjun Nair", "arjun.nair", "IBI2024009", "B.Tech IT-BI", "2", "0"},
        {"Divya Mishra", "divya.mishra", "IEC2024010", "B.Tech ECE", "2", "1"},
        {"Siddharth Jain", "siddharth.jain", "IIT2025011", "B.Tech IT", "1", "0"},
        {"Meera Pillai", "meera.pillai", "IIT2025012", "B.Tech IT", "1", "1"},
        {"Harsh Agarwal", "harsh.agarwal", "IEC2025013", "B.Tech ECE", "1", "0"},
        {"Ishaan Malhotra", "ishaan.malhotra", "IBI2025014", "B.Tech IT-BI", "1", "0"},
        {"Pranav Deshmukh", "pranav.deshmukh", "IIT2023015", "B.Tech IT", "3", "0"},
        {"Aditya Kulkarni", "aditya.kulkarni", "IIT2022016", "B.Tech IT", "4", "0"},
        {"Manish Tiwari", "manish.tiwari", "IEC2024017", "B.Tech ECE", "2", "0"},
        {"Rahul Bansal", "rahul.bansal", "IIT2025018", "B.Tech IT", "1", "0"},
        {"Tanvi Saxena", "tanvi.saxena", "IIT2024019", "B.Tech IT", "2", "1"},
        {"Yash Choudhary", "yash.choudhary", "IBI2023020", "B.Tech IT-BI", "3", "0"},
        {"Karan Mehta", "karan.mehta", "IIT2025021", "B.Tech IT", "1", "-1"},   // new admission, not yet allocated
    };

    private final UserService users;
    private final ResidentService residents;
    private final AccommodationService accommodation;
    private final MealScheduleService schedule;
    private final MessOperationsService messOps;
    private final MessDemoData messDemo;
    private final LedgerService ledger;
    private final CleaningDemoData cleaningDemo;
    private final ComplaintDemoData complaintDemo;
    private final DashboardService dashboard;
    private final boolean demoMode;
    private final String password;

    public DemoDataSeeder(UserService users, ResidentService residents, AccommodationService accommodation,
                          MealScheduleService schedule, MessOperationsService messOps, MessDemoData messDemo,
                          LedgerService ledger, CleaningDemoData cleaningDemo, ComplaintDemoData complaintDemo,
                          DashboardService dashboard, @Value("${hostelos.demo-mode}") boolean demoMode,
                          @Value("${hostelos.demo-password}") String password) {
        this.users = users;
        this.residents = residents;
        this.accommodation = accommodation;
        this.schedule = schedule;
        this.messOps = messOps;
        this.messDemo = messDemo;
        this.ledger = ledger;
        this.cleaningDemo = cleaningDemo;
        this.complaintDemo = complaintDemo;
        this.dashboard = dashboard;
        this.demoMode = demoMode;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now();
        // Always keep a rolling window of meals (past week + next week).
        boolean firstRun = demoMode && users.count() == 0;
        schedule.ensureMeals(firstRun ? today.minusDays(14) : today.minusDays(1), today.plusDays(7));
        messOps.closeStaleBookings();
        if (!firstRun) return;

        log.info("Seeding HostelOS demo data...");
        User admin = users.create("admin@hostelos.in", password, "Dr. Meera Nair", Role.ADMIN);
        users.create("warden@hostelos.in", password, "Rajesh Kulkarni", Role.WARDEN);
        User mess = users.create("mess@hostelos.in", password, "Sunil Yadav", Role.MESS_STAFF);
        User hk1 = users.create("housekeeping@hostelos.in", password, "Lakshmi Devi", Role.HOUSEKEEPING_STAFF);
        User hk2 = users.create("housekeeping2@hostelos.in", password, "Ramesh Kumar", Role.HOUSEKEEPING_STAFF);
        User mt1 = users.create("maintenance@hostelos.in", password, "Imran Sheikh", Role.MAINTENANCE_STAFF);
        User mt2 = users.create("maintenance2@hostelos.in", password, "Suresh Patel", Role.MAINTENANCE_STAFF);

        // ---- accommodation: two hostels
        var aryabhatta = accommodation.createHostel("Aryabhatta Bhawan", "Main boys' hostel, 3 floors");
        var kalpana = accommodation.createHostel("Kalpana Bhawan", "Girls' hostel, 2 floors");
        for (int floor = 1; floor <= 3; floor++) for (int n = 1; n <= 5; n++)
            accommodation.addRoom(aryabhatta.getId(), floor, floor + "0" + n, 2);
        for (int floor = 1; floor <= 2; floor++) for (int n = 1; n <= 4; n++)
            accommodation.addRoom(kalpana.getId(), floor, floor + "0" + n, 2);
        accommodation.addRoom(aryabhatta.getId(), 0, "001", 3);       // a ground-floor triple

        // ---- residents + allocations (filled room by room, so roommates are paired)
        List<HostelView> overview = accommodation.overview();
        Map<Integer, Deque<Long>> freeBeds = new HashMap<>();
        for (int h = 0; h < 2; h++) {
            Long hostelId = h == 0 ? aryabhatta.getId() : kalpana.getId();
            HostelView view = overview.stream().filter(v -> v.id().equals(hostelId)).findFirst().orElseThrow();
            Deque<Long> beds = new ArrayDeque<>();
            view.rooms().stream().filter(r -> r.floorNo() > 0).forEach(r -> r.beds().forEach(b -> beds.add(b.id())));
            freeBeds.put(h, beds);
        }
        List<Long> residentIds = new ArrayList<>();
        for (String[] s : STUDENTS) {
            User u = users.create(s[1] + "@iiita.ac.in", password, s[0], Role.STUDENT);
            Resident r = residents.register(u, s[2], "+91 90000 000" + s[2].substring(s[2].length() - 2), s[3], Integer.parseInt(s[4]));
            residentIds.add(r.getId());
            int h = Integer.parseInt(s[5]);
            if (h >= 0) accommodation.allocate(r.getId(), freeBeds.get(h).poll());
        }
        List<Long> allocated = residentIds.subList(0, residentIds.size() - 1);   // everyone except the new admission
        Set<Long> demoResidents = Set.of(residentIds.get(0), residentIds.get(1));

        // ---- mess history (real billing path) then money
        messDemo.seed(allocated, demoResidents, mess.getId());
        seedFinance(allocated);

        // ---- services
        cleaningDemo.seed(allocated, List.of(hk1.getId(), hk2.getId()));
        complaintDemo.seed(allocated, List.of(mt1.getId(), mt2.getId()));

        // ---- announcements
        dashboard.announce("Mess menu revised from Monday",
                "Based on resident feedback the dinner menu now includes a weekly special. Pay-per-meal booking stays open until 90 minutes before each meal.", admin.getId());
        dashboard.announce("Water supply maintenance on Saturday",
                "Water will be unavailable in Aryabhatta Bhawan between 10 AM and 1 PM for tank cleaning.", admin.getId());
        dashboard.announce("Room cleaning slots now bookable online",
                "Book a cleaning slot for your room from the Room services page. Slots are first come, first served.", admin.getId());
        dashboard.announce("Hostel night curfew reminder",
                "Hostel gates close at 11:30 PM. Late entry requires prior approval from the warden.", admin.getId());
        log.info("Demo data ready. Accounts use the password '{}'.", password);
    }

    /** Rent for last month and this month, and a deterministic mix of payments, all through the ledger. */
    private void seedFinance(List<Long> residentIds) {
        Random rnd = new Random(7);
        YearMonth thisMonth = YearMonth.now(), lastMonth = thisMonth.minusMonths(1);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal rent = new BigDecimal("3500.00");
        for (Long rid : residentIds) {
            ledger.postCharge(new ChargePosting(rid, rent, "Room rent – " + lastMonth, LedgerService.MODULE_RENT,
                    "RENT-" + rid + "-" + lastMonth, lastMonth.atDay(1).atTime(LocalTime.of(10, 0))));
            LocalDateTime thisRentAt = thisMonth.atDay(1).atTime(LocalTime.of(10, 0));
            if (thisRentAt.isAfter(now)) thisRentAt = now.minusMinutes(1);
            ledger.postCharge(new ChargePosting(rid, rent, "Room rent – " + thisMonth, LedgerService.MODULE_RENT,
                    "RENT-" + rid + "-" + thisMonth, thisRentAt));
            double roll = rnd.nextDouble();
            if (roll < 0.8) ledger.postPayment(rid, rent, "UPI", "Rent " + lastMonth, lastMonth.atDay(6).atTime(LocalTime.of(18, 30)));
            if (rnd.nextDouble() < 0.3) ledger.postPayment(rid, new BigDecimal("500.00"), "UPI", "Mess dues", now.minusDays(2 + rnd.nextInt(4)));
        }
    }
}
