package com.hostelos.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CheckoutIntegrationTest extends IntegrationTestBase {

    @Test
    void duesBlockClearanceAndClearingKeepsHistory() throws Exception {
        String student = token("tanvi.saxena@iiita.ac.in"), warden = token(WARDEN), admin = token(ADMIN);
        long residentId = jdbc.queryForObject("select r.id from residents r join users u on u.id=r.user_id where u.email='tanvi.saxena@iiita.ac.in'", Long.class);
        long ledgerBefore = jdbc.queryForObject("select count(*) from ledger_entries where resident_id=?", Long.class, residentId);

        var created = call(student, post("/api/checkout").content("{\"plannedDate\":\"" + LocalDate.now().plusDays(5) + "\"}"));
        assertThat(created.getResponse().getStatus()).isEqualTo(200);
        long id = body(created).get("id").asLong();
        assertThat(status(student, post("/api/checkout").content("{\"plannedDate\":\"" + LocalDate.now().plusDays(6) + "\"}"))).as("one pending request at a time").isEqualTo(409);

        double dues = outstanding(student);
        assertThat(dues).as("seed gives this resident unpaid rent").isPositive();
        assertThat(status(warden, post("/api/checkout/" + id + "/clear").content("{}"))).as("blocked by dues").isEqualTo(409);
        assertThat(status(student, post("/api/checkout/" + id + "/clear").content("{}"))).as("students cannot clear").isEqualTo(403);

        assertThat(status(admin, post("/api/finance/residents/" + residentId + "/payments").content("{\"amount\":" + dues + ",\"method\":\"cash\"}"))).isEqualTo(200);
        assertThat(status(warden, post("/api/checkout/" + id + "/clear").content("{\"note\":\"All clear\"}"))).isEqualTo(200);

        assertThat(jdbc.queryForObject("select status from residents where id=?", String.class, residentId)).isEqualTo("CHECKED_OUT");
        assertThat(jdbc.queryForObject("select count(*) from allocations where resident_id=? and end_date is null", Long.class, residentId)).as("bed released").isZero();
        assertThat(jdbc.queryForObject("select count(*) from allocations where resident_id=?", Long.class, residentId)).as("allocation history kept").isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from ledger_entries where resident_id=?", Long.class, residentId)).as("ledger kept").isGreaterThan(ledgerBefore);
        assertThat(status(warden, post("/api/checkout/" + id + "/clear").content("{}"))).as("cannot clear twice").isEqualTo(409);
        assertThat(status(student, get("/api/checkout/mine"))).isEqualTo(200);
    }
}
