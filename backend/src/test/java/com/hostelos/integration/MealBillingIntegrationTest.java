package com.hostelos.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Workflow A: booking -> staff confirmation -> consumption -> exactly one ledger charge. */
class MealBillingIntegrationTest extends IntegrationTestBase {

    @Test
    void bookingAloneNeverChargesAndConfirmationChargesExactlyOnce() throws Exception {
        String student = token(STUDENT), staff = token(MESS);
        long meal = openMealToday(student, "SNACKS");
        double before = outstanding(student);

        assertThat(status(student, post("/api/meals/" + meal + "/book"))).isEqualTo(200);
        assertThat(outstanding(student)).as("a booking is not a charge").isEqualTo(before);

        long booking = bookingId(staff, meal, "Aarav Sharma");
        assertThat(status(staff, post("/api/mess/bookings/" + booking + "/confirm"))).isEqualTo(200);
        double price = jdbc.queryForObject("select price from meals where id=?", Double.class, meal);
        assertThat(outstanding(student)).isEqualTo(before + price);
        assertThat(ledgerRows(booking)).isEqualTo(1);

        assertThat(status(staff, post("/api/mess/bookings/" + booking + "/confirm"))).as("duplicate confirmation").isEqualTo(409);
        assertThat(ledgerRows(booking)).as("still one charge").isEqualTo(1);
        assertThat(outstanding(student)).isEqualTo(before + price);
    }

    @Test
    void cancelledBookingCannotBeBilled() throws Exception {
        String student = token(STUDENT), staff = token(MESS);
        long meal = openMealToday(student, "BREAKFAST");
        double before = outstanding(student);
        assertThat(status(student, post("/api/meals/" + meal + "/book"))).isEqualTo(200);
        assertThat(status(student, post("/api/meals/" + meal + "/cancel"))).isEqualTo(200);

        long booking = bookingId(staff, meal, "Aarav Sharma");
        assertThat(status(staff, post("/api/mess/bookings/" + booking + "/confirm"))).isEqualTo(409);
        assertThat(ledgerRows(booking)).isZero();
        assertThat(outstanding(student)).isEqualTo(before);
    }

    @Test
    void concurrentConfirmationsProduceOneChargeOnly() throws Exception {
        String student = token(STUDENT_2), staff = token(MESS);
        long meal = openMealToday(student, "DINNER");
        assertThat(status(student, post("/api/meals/" + meal + "/book"))).isEqualTo(200);
        long booking = bookingId(staff, meal, "Priya Verma");

        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Callable<Integer> task = () -> status(staff, post("/api/mess/bookings/" + booking + "/confirm"));
            results.add(pool.submit(task));
        }
        int ok = 0;
        for (Future<Integer> f : results) if (f.get() == 200) ok++;
        pool.shutdown();

        assertThat(ok).as("exactly one confirmation wins").isEqualTo(1);
        assertThat(ledgerRows(booking)).isEqualTo(1);
    }

    @Test
    void feedbackRequiresEatingTheMealAndPersistsForAdmins() throws Exception {
        String student = token(STUDENT_2), staff = token(MESS), admin = token(ADMIN);
        long meal = openMealToday(student, "LUNCH");
        String review = "{\"tasteRating\":4,\"hygieneRating\":2,\"comment\":\"Plates were wet\"}";
        assertThat(status(student, post("/api/meals/" + meal + "/book"))).isEqualTo(200);
        assertThat(status(student, post("/api/meals/" + meal + "/feedback").content(review))).as("booked but not eaten").isEqualTo(409);

        assertThat(status(staff, post("/api/mess/bookings/" + bookingId(staff, meal, "Priya Verma") + "/confirm"))).isEqualTo(200);
        assertThat(status(student, post("/api/meals/" + meal + "/feedback").content(review))).isEqualTo(200);
        assertThat(status(student, post("/api/meals/" + meal + "/feedback").content("{\"tasteRating\":9,\"hygieneRating\":2}"))).isEqualTo(400);

        String feedback = call(admin, get("/api/mess/feedback?limit=200")).getResponse().getContentAsString();
        assertThat(feedback).contains("Plates were wet");
    }

    @Test
    void ledgerRowsCannotBeEditedOrDeleted() {
        Long id = jdbc.queryForObject("select min(id) from ledger_entries", Long.class);
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> jdbc.update("update ledger_entries set amount = 1 where id = ?", id));
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> jdbc.update("delete from ledger_entries where id = ?", id));
    }
}
