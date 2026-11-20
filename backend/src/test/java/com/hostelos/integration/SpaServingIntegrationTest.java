package com.hostelos.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;

/** Without a built frontend on the classpath, API paths still 404 cleanly and the gate is inactive by default. */
class SpaServingIntegrationTest extends IntegrationTestBase {

    @Test
    void healthCheckAnswersWithoutLogin() throws Exception {
        assertThat(mvc.perform(get("/healthz")).andReturn().getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void unknownApiPathsAreNotSwallowedByTheSinglePageAppFallback() throws Exception {
        String token = token(ADMIN);
        int status = status(token, get("/api/does-not-exist"));
        assertThat(status).isEqualTo(404);
    }

    @Test
    void gateIsOffByDefault() throws Exception {
        assertThat(mvc.perform(get("/api/auth/demo-accounts")).andReturn().getResponse().getStatus()).isEqualTo(200);
    }
}
