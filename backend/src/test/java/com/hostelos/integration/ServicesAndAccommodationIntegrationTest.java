package com.hostelos.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ServicesAndAccommodationIntegrationTest extends IntegrationTestBase {

    @Test
    void cleaningSlotCannotBeDoubleBookedAndFollowsTheWorkflow() throws Exception {
        String student = token(STUDENT), roommate = token("rohan.gupta@iiita.ac.in"), staff = token(HOUSEKEEPING);
        String date = LocalDate.now().plusDays(9).toString();
        String req = "{\"date\":\"" + date + "\",\"slot\":\"AFTERNOON\",\"instructions\":\"Windows please\"}";

        var created = call(student, post("/api/cleaning").content(req));
        assertThat(created.getResponse().getStatus()).isEqualTo(200);
        long id = body(created).get("id").asLong();
        assertThat(body(created).get("status").asText()).isEqualTo("SCHEDULED");
        assertThat(status(roommate, post("/api/cleaning").content(req))).as("same room, same slot").isEqualTo(409);

        assertThat(status(staff, patch("/api/cleaning/" + id + "/status").content("{\"status\":\"COMPLETED\"}"))).as("not assigned to this staff member").isEqualTo(403);
        assertThat(status(staff, post("/api/cleaning/" + id + "/claim"))).isEqualTo(200);
        assertThat(status(staff, patch("/api/cleaning/" + id + "/status").content("{\"status\":\"COMPLETED\"}"))).as("cannot skip IN_PROGRESS").isEqualTo(409);
        assertThat(status(staff, patch("/api/cleaning/" + id + "/status").content("{\"status\":\"IN_PROGRESS\"}"))).isEqualTo(200);
        assertThat(status(staff, patch("/api/cleaning/" + id + "/status").content("{\"status\":\"COMPLETED\",\"notes\":\"Done\"}"))).isEqualTo(200);

        JsonNode mine = body(call(student, get("/api/cleaning/mine")));
        boolean completed = false;
        for (JsonNode c : mine) if (c.get("id").asLong() == id) completed = c.get("status").asText().equals("COMPLETED");
        assertThat(completed).as("student sees the final status").isTrue();
    }

    @Test
    void cancellingFreesTheSlot() throws Exception {
        String student = token(STUDENT);
        String date = LocalDate.now().plusDays(10).toString();
        String req = "{\"date\":\"" + date + "\",\"slot\":\"EVENING\"}";
        long id = body(call(student, post("/api/cleaning").content(req))).get("id").asLong();
        assertThat(status(student, post("/api/cleaning/" + id + "/cancel"))).isEqualTo(200);
        assertThat(status(student, post("/api/cleaning").content(req))).as("slot can be re-booked").isEqualTo(200);
    }

    @Test
    void maintenanceComplaintMovesThroughControlledStates() throws Exception {
        String student = token(STUDENT), staff = token(MAINTENANCE);
        long id = body(call(student, post("/api/maintenance").content("{\"category\":\"ELECTRICAL\",\"priority\":\"HIGH\",\"description\":\"Socket sparks\"}"))).get("id").asLong();
        String patchUrl = "/api/maintenance/" + id + "/status";

        assertThat(status(staff, patch(patchUrl).content("{\"status\":\"RESOLVED\",\"notes\":\"x\"}"))).as("not assigned yet").isEqualTo(403);
        assertThat(status(staff, post("/api/maintenance/" + id + "/claim"))).isEqualTo(200);
        assertThat(status(staff, patch(patchUrl).content("{\"status\":\"RESOLVED\",\"notes\":\"x\"}"))).as("must start first").isEqualTo(409);
        assertThat(status(staff, patch(patchUrl).content("{\"status\":\"IN_PROGRESS\"}"))).isEqualTo(200);
        assertThat(status(staff, patch(patchUrl).content("{\"status\":\"RESOLVED\"}"))).as("resolution notes required").isEqualTo(400);
        assertThat(status(staff, patch(patchUrl).content("{\"status\":\"RESOLVED\",\"notes\":\"Replaced socket\"}"))).isEqualTo(200);
        assertThat(status(staff, patch(patchUrl).content("{\"status\":\"CLOSED\"}"))).as("staff cannot close").isEqualTo(403);
        assertThat(status(student, post("/api/maintenance/" + id + "/close"))).isEqualTo(200);
        assertThat(status(student, post("/api/maintenance/" + id + "/close"))).as("closed is final").isEqualTo(409);
    }

    @Test
    void bedCannotBeDoubleAllocatedAndTransfersKeepHistory() throws Exception {
        String warden = token(WARDEN);
        long newcomer = jdbc.queryForObject("select r.id from residents r where not exists (select 1 from allocations a where a.resident_id=r.id and a.end_date is null) limit 1", Long.class);
        long occupiedBed = jdbc.queryForObject("select bed_id from allocations where end_date is null limit 1", Long.class);
        long freeBed = jdbc.queryForObject("select b.id from beds b where not exists (select 1 from allocations a where a.bed_id=b.id and a.end_date is null) order by b.id limit 1", Long.class);
        long freeBed2 = jdbc.queryForObject("select b.id from beds b where not exists (select 1 from allocations a where a.bed_id=b.id and a.end_date is null) and b.id <> ? order by b.id limit 1", Long.class, freeBed);

        assertThat(status(warden, post("/api/accommodation/allocate").content("{\"residentId\":" + newcomer + ",\"bedId\":" + occupiedBed + "}"))).as("occupied bed").isEqualTo(409);
        assertThat(status(warden, post("/api/accommodation/allocate").content("{\"residentId\":" + newcomer + ",\"bedId\":" + freeBed + "}"))).isEqualTo(200);
        assertThat(status(warden, post("/api/accommodation/allocate").content("{\"residentId\":" + newcomer + ",\"bedId\":" + freeBed2 + "}"))).as("already has a bed").isEqualTo(409);

        assertThat(status(warden, post("/api/accommodation/transfer").content("{\"residentId\":" + newcomer + ",\"bedId\":" + freeBed2 + "}"))).isEqualTo(200);
        assertThat(jdbc.queryForObject("select count(*) from allocations where resident_id=?", Long.class, newcomer)).as("history is kept").isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from allocations where resident_id=? and end_date is null", Long.class, newcomer)).isEqualTo(1);

        assertThat(status(warden, post("/api/accommodation/release").content("{\"residentId\":" + newcomer + "}"))).isEqualTo(200);
        assertThat(jdbc.queryForObject("select count(*) from allocations where resident_id=? and end_date is null", Long.class, newcomer)).isZero();
    }

    @Test
    void rentPostingIsIdempotent() throws Exception {
        String admin = token(ADMIN);
        String month = "2031-01";
        int first = body(call(admin, post("/api/finance/rent").content("{\"month\":\"" + month + "\"}"))).get("posted").asInt();
        int second = body(call(admin, post("/api/finance/rent").content("{\"month\":\"" + month + "\"}"))).get("posted").asInt();
        assertThat(first).isPositive();
        assertThat(second).isZero();
    }
}
