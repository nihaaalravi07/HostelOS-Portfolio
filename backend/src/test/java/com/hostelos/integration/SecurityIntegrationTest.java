package com.hostelos.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        assertThat(mvc.perform(get("/api/dashboard/student")).andReturn().getResponse().getStatus()).isEqualTo(401);
        assertThat(mvc.perform(get("/api/dashboard/student").header("Authorization", "Bearer garbage")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        int s = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + STUDENT + "\",\"password\":\"nope\"}")).andReturn().getResponse().getStatus();
        assertThat(s).isEqualTo(401);
    }

    @Test
    void everyRoleCanSignIn() throws Exception {
        for (String email : new String[]{STUDENT, ADMIN, WARDEN, MESS, HOUSEKEEPING, MAINTENANCE}) assertThat(token(email)).isNotBlank();
    }

    @Test
    void studentsCannotReachStaffOrAdminEndpoints() throws Exception {
        String t = token(STUDENT);
        for (String url : new String[]{"/api/dashboard/admin", "/api/mess/board", "/api/finance/report", "/api/accommodation/overview",
                "/api/maintenance/board", "/api/cleaning/board", "/api/users"}) {
            assertThat(status(t, get(url))).as(url).isEqualTo(403);
        }
        assertThat(status(t, post("/api/mess/bookings/1/confirm"))).isEqualTo(403);
    }

    @Test
    void staffRolesAreLimitedToTheirOwnArea() throws Exception {
        assertThat(status(token(MESS), get("/api/finance/report"))).isEqualTo(403);
        assertThat(status(token(WARDEN), get("/api/finance/report"))).isEqualTo(403);
        assertThat(status(token(HOUSEKEEPING), get("/api/maintenance/board"))).isEqualTo(403);
        assertThat(status(token(MAINTENANCE), get("/api/cleaning/board"))).isEqualTo(403);
        assertThat(status(token(ADMIN), get("/api/meals"))).as("admin is not a resident").isEqualTo(403);
        assertThat(status(token(ADMIN), get("/api/finance/report"))).isEqualTo(200);
    }

    @Test
    void aStudentSeesOnlyTheirOwnFinancesAndRoom() throws Exception {
        String a = token(STUDENT), b = token(STUDENT_2);
        assertThat(body(call(a, get("/api/accommodation/mine"))).get("hostelName").asText()).isEqualTo("Aryabhatta Bhawan");
        assertThat(body(call(b, get("/api/accommodation/mine"))).get("hostelName").asText()).isEqualTo("Kalpana Bhawan");
        assertThat(body(call(a, get("/api/dashboard/student"))).get("profile").get("fullName").asText()).isEqualTo("Aarav Sharma");
        // the resident is always derived from the token; there is no way to ask for someone else's statement
        assertThat(status(a, get("/api/finance/residents/1/statement"))).isEqualTo(403);
    }
}
