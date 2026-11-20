package com.hostelos.config;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cheap liveness probe for the hosting platform. Deliberately touches no database and needs no login. */
@RestController
public class HealthController {
    @GetMapping("/healthz")
    public Map<String, String> health() { return Map.of("status", "ok"); }
}
