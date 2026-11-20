package com.hostelos.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Boots the whole application against a throw-away PostgreSQL (no Docker needed), including Flyway
 * migrations and the deterministic demo seed. All integration tests share this one context.
 */
@SpringBootTest(properties = {
        "hostelos.jwt-secret=test-only-secret-not-for-real-use-0123456789abcdef",
        "hostelos.demo-mode=true"})
@AutoConfigureMockMvc
abstract class IntegrationTestBase {
    static final EmbeddedPostgres PG;

    static {
        try {
            PG = EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;

    protected static final String STUDENT = "aarav.sharma@iiita.ac.in";
    protected static final String STUDENT_2 = "priya.verma@iiita.ac.in";
    protected static final String ADMIN = "admin@hostelos.in";
    protected static final String WARDEN = "warden@hostelos.in";
    protected static final String MESS = "mess@hostelos.in";
    protected static final String HOUSEKEEPING = "housekeeping@hostelos.in";
    protected static final String MAINTENANCE = "maintenance@hostelos.in";

    protected String token(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"hostelos123\"}")).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    protected MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    protected MvcResult call(String token, MockHttpServletRequestBuilder b) throws Exception {
        return mvc.perform(as(token, b)).andReturn();
    }

    protected JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    protected int status(String token, MockHttpServletRequestBuilder b) throws Exception {
        return call(token, b).getResponse().getStatus();
    }

    /** First meal today in this category that the student has not booked. */
    protected long openMealToday(String studentToken, String category) throws Exception {
        String today = LocalDate.now().toString();
        for (JsonNode m : body(call(studentToken, get("/api/meals?from=" + today + "&to=" + today)))) {
            if (m.get("category").asText().equals(category) && m.get("myStatus").asText().equals("NOT_BOOKED")) return m.get("id").asLong();
        }
        throw new IllegalStateException("no free " + category + " meal today");
    }

    protected long bookingId(String staffToken, long mealId, String residentName) throws Exception {
        for (JsonNode board : body(call(staffToken, get("/api/mess/board?date=" + LocalDate.now()))).get("meals")) {
            if (board.get("meal").get("id").asLong() != mealId) continue;
            for (JsonNode b : board.get("bookings")) if (b.get("residentName").asText().equals(residentName)) return b.get("bookingId").asLong();
        }
        throw new IllegalStateException("booking not found");
    }

    protected double outstanding(String studentToken) throws Exception {
        return body(call(studentToken, get("/api/finance/me/summary"))).get("outstanding").asDouble();
    }

    protected long ledgerRows(long bookingId) {
        return jdbc.queryForObject("select count(*) from ledger_entries where source_module='MESS' and source_ref=?", Long.class, "BOOKING-" + bookingId);
    }
}
