package com.hostelos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)   // we authenticate with JWTs, not Spring's default user
@ConfigurationPropertiesScan
public class HostelOsApplication {
    public static void main(String[] args) {
        SpringApplication.run(HostelOsApplication.class, args);
    }
}
