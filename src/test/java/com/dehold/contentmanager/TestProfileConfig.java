package com.dehold.contentmanager;

import org.springframework.boot.test.context.TestConfiguration;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class TestProfileConfig {

    @PostConstruct
    public void setTestProfile() {
        System.setProperty("spring.profiles.active", "h2");
    }

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {

        // NEW DSL — no deprecated csrf()
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())           // ← not deprecated
                .httpBasic(Customizer.withDefaults())   // optional (not used)
                .formLogin(form -> form.disable());     // disable login page

        return http.build();
    }
}
