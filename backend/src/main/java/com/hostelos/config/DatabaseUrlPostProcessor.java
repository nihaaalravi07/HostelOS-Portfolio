package com.hostelos.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Hosting providers (Neon, Render, Heroku-style platforms) hand out ONE connection string like
 *   postgresql://user:password@host/dbname?sslmode=require
 * but JDBC wants a different shape. If DATABASE_URL is set we translate it, so deployers just paste it.
 * Explicit DB_URL / DB_USER / DB_PASSWORD still work and take precedence.
 */
public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String raw = env.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank() || env.getProperty("DB_URL") != null) return;
        Map<String, Object> props = translate(raw.trim());
        if (!props.isEmpty()) env.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
    }

    /** Neon's copy-paste string ends in channel_binding=require, which the JDBC driver does not understand. */
    private static String cleanQuery(String rawQuery) {
        if (rawQuery == null) return "";
        String kept = java.util.Arrays.stream(rawQuery.split("&"))
                .filter(p -> !p.isBlank() && !p.startsWith("channel_binding=")).collect(java.util.stream.Collectors.joining("&"));
        return kept.isEmpty() ? "" : "?" + kept;
    }

    /** Package-visible for unit testing. */
    static Map<String, Object> translate(String raw) {
        Map<String, Object> props = new HashMap<>();
        if (raw.startsWith("jdbc:")) { props.put("spring.datasource.url", raw); return props; }
        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) return props;
        URI uri = URI.create(raw.replaceFirst("^postgres(ql)?://", "http://"));
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String query = cleanQuery(uri.getRawQuery());
        props.put("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query);
        if (uri.getRawUserInfo() != null) {
            String[] parts = uri.getRawUserInfo().split(":", 2);
            props.put("spring.datasource.username", URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
            if (parts.length > 1) props.put("spring.datasource.password", URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return props;
    }
}
