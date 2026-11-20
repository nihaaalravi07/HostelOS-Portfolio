package com.hostelos.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Workflow A end to end, observed from the user's side: a booking is made, staff confirm it, and the student
 * dashboard, the mess board and the admin dashboard all move by exactly the right amounts.
 */
class MealWorkflowEndToEndIntegrationTest extends IntegrationTestBase {

    @Test
    void bookingThenConfirmationUpdatesStudentMessAndAdminViewsByTheMealPrice() throws Exception {
        String student = token(STUDENT), staff = token(MESS), admin = token(ADMIN);
        long meal = openMealToday(student, "LUNCH");
        double price = jdbc.queryForObject("select price from meals where id=?", Double.class, meal);

        JsonNode sBefore = body(call(student, get("/api/dashboard/student")));
        JsonNode aBefore = body(call(admin, get("/api/dashboard/admin")));
        JsonNode bBefore = body(call(staff, get("/api/mess/board?date=" + LocalDate.now()))).get("totals");
        assertThat(sBefore.get("account").get("outstanding").asDouble()).as("seeded resident owes money").isPositive();

        // 1. booking: demand rises, nothing is billed
        assertThat(status(student, post("/api/meals/" + meal + "/book"))).isEqualTo(200);
        JsonNode aBooked = body(call(admin, get("/api/dashboard/admin")));
        assertThat(aBooked.get("todayExpected").asLong()).isEqualTo(aBefore.get("todayExpected").asLong() + 1);
        assertThat(aBooked.get("todayPending").asLong()).isEqualTo(aBefore.get("todayPending").asLong() + 1);
        assertThat(aBooked.get("todayRevenue").asDouble()).as("no revenue until eaten").isEqualTo(aBefore.get("todayRevenue").asDouble());
        assertThat(body(call(student, get("/api/dashboard/student"))).get("account").get("outstanding").asDouble())
                .isEqualTo(sBefore.get("account").get("outstanding").asDouble());

        // 2. confirmation: pending becomes consumed, money moves exactly once
        long booking = bookingId(staff, meal, "Aarav Sharma");
        assertThat(status(staff, post("/api/mess/bookings/" + booking + "/confirm"))).isEqualTo(200);
        assertThat(status(staff, post("/api/mess/bookings/" + booking + "/confirm"))).as("second click").isEqualTo(409);

        JsonNode sAfter = body(call(student, get("/api/dashboard/student")));
        assertThat(sAfter.get("messSpendThisMonth").asDouble()).isEqualTo(sBefore.get("messSpendThisMonth").asDouble() + price);
        assertThat(sAfter.get("mealsConsumedThisMonth").asLong()).isEqualTo(sBefore.get("mealsConsumedThisMonth").asLong() + 1);
        assertThat(sAfter.get("account").get("totalCharges").asDouble()).isEqualTo(sBefore.get("account").get("totalCharges").asDouble() + price);
        assertThat(sAfter.get("account").get("outstanding").asDouble()).isEqualTo(sBefore.get("account").get("outstanding").asDouble() + price);

        JsonNode aAfter = body(call(admin, get("/api/dashboard/admin")));
        assertThat(aAfter.get("todayConsumed").asLong()).isEqualTo(aBefore.get("todayConsumed").asLong() + 1);
        assertThat(aAfter.get("todayPending").asLong()).as("back to the starting value").isEqualTo(aBefore.get("todayPending").asLong());
        assertThat(aAfter.get("todayRevenue").asDouble()).isEqualTo(aBefore.get("todayRevenue").asDouble() + price);
        assertThat(aAfter.get("monthRevenue").asDouble()).isEqualTo(aBefore.get("monthRevenue").asDouble() + price);
        assertThat(aAfter.get("outstandingBalances").asDouble()).isEqualTo(aBefore.get("outstandingBalances").asDouble() + price);

        JsonNode bAfter = body(call(staff, get("/api/mess/board?date=" + LocalDate.now()))).get("totals");
        assertThat(bAfter.get("consumed").asLong()).isEqualTo(bBefore.get("consumed").asLong() + 1);
        assertThat(ledgerRows(booking)).isEqualTo(1);
    }

    @Test
    void theDatabaseItselfRejectsASecondChargeForTheSameSource() {
        Long resident = jdbc.queryForObject("select min(id) from residents", Long.class);
        jdbc.update("insert into ledger_entries(resident_id,type,amount,description,created_at,source_module,source_ref) values (?,?,?,?,now(),?,?)",
                resident, "CHARGE", 10, "first", "TEST", "UNIQUE-REF-1");
        assertThatThrownBy(() -> jdbc.update(
                "insert into ledger_entries(resident_id,type,amount,description,created_at,source_module,source_ref) values (?,?,?,?,now(),?,?)",
                resident, "CHARGE", 10, "duplicate", "TEST", "UNIQUE-REF-1"))
                .hasMessageContaining("uq_ledger_source");
    }
}
