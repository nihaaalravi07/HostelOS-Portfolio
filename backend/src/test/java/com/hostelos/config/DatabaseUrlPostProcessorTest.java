package com.hostelos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DatabaseUrlPostProcessorTest {

    @Test
    void translatesANeonStyleUrl() {
        Map<String, Object> p = DatabaseUrlPostProcessor.translate(
                "postgresql://app_user:s3cret@ep-cool-123.ap-southeast-1.aws.neon.tech/neondb?sslmode=require");
        assertThat(p.get("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-cool-123.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require");
        assertThat(p.get("spring.datasource.username")).isEqualTo("app_user");
        assertThat(p.get("spring.datasource.password")).isEqualTo("s3cret");
    }

    @Test
    void acceptsThePostgresSchemeExplicitPortAndEncodedPassword() {
        Map<String, Object> p = DatabaseUrlPostProcessor.translate("postgres://u:p%40ss%2Fword@db.example.com:6543/mydb");
        assertThat(p.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://db.example.com:6543/mydb");
        assertThat(p.get("spring.datasource.password")).isEqualTo("p@ss/word");
    }

    @Test
    void dropsNeonChannelBindingParameter() {
        Map<String, Object> p = DatabaseUrlPostProcessor.translate("postgresql://u:p@ep-x.neon.tech/neondb?sslmode=require&channel_binding=require");
        assertThat(p.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://ep-x.neon.tech:5432/neondb?sslmode=require");
        assertThat(DatabaseUrlPostProcessor.translate("postgresql://u:p@h/db?channel_binding=require").get("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://h:5432/db");
    }

    @Test
    void passesThroughJdbcUrlsAndIgnoresGarbage() {
        assertThat(DatabaseUrlPostProcessor.translate("jdbc:postgresql://h/db").get("spring.datasource.url")).isEqualTo("jdbc:postgresql://h/db");
        assertThat(DatabaseUrlPostProcessor.translate("mysql://x/y")).isEmpty();
    }
}
