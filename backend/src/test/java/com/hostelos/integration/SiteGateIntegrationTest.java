package com.hostelos.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/** The passcode gate used on a public deployment. A separate Spring context with SITE_PASSCODE set. */
@TestPropertySource(properties = "hostelos.site-passcode=open-sesame-42")
class SiteGateIntegrationTest extends IntegrationTestBase {

    @Test
    void visitorsWithoutTheCookieSeeThePasscodePageNotTheApp() throws Exception {
        MvcResult page = mvc.perform(get("/meals")).andReturn();
        assertThat(page.getResponse().getContentAsString()).contains("Enter the access passcode");

        MvcResult api = mvc.perform(get("/api/auth/demo-accounts")).andReturn();
        assertThat(api.getResponse().getStatus()).isEqualTo(401);
        assertThat(api.getResponse().getContentAsString()).contains("Passcode required");
        assertThat(mvc.perform(get("/swagger-ui/index.html")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void healthCheckStaysOpenForThePlatform() throws Exception {
        MvcResult r = mvc.perform(get("/healthz")).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(r.getResponse().getContentAsString()).contains("ok");
    }

    @Test
    void wrongPasscodeIsRejectedAndRightPasscodeUnlocks() throws Exception {
        MvcResult wrong = mvc.perform(post("/gate").param("passcode", "nope")).andReturn();
        assertThat(wrong.getResponse().getCookie("hostelos_gate")).isNull();
        assertThat(wrong.getResponse().getContentAsString()).contains("not right");

        MvcResult right = mvc.perform(post("/gate").param("passcode", "open-sesame-42")).andReturn();
        Cookie cookie = right.getResponse().getCookie("hostelos_gate");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(right.getResponse().getStatus()).isEqualTo(302);

        MvcResult unlocked = mvc.perform(get("/api/auth/demo-accounts").cookie(cookie)).andReturn();
        assertThat(unlocked.getResponse().getStatus()).isEqualTo(200);
        assertThat(unlocked.getResponse().getContentAsString()).contains("aarav.sharma@iiita.ac.in");
    }

    @Test
    void aForgedCookieDoesNotWork() throws Exception {
        MvcResult r = mvc.perform(get("/api/auth/demo-accounts").cookie(new Cookie("hostelos_gate", "forged"))).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(401);
    }
}
