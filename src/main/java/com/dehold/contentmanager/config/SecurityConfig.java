package com.dehold.contentmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    // 1. Password Encoder: Required to securely store and verify passwords.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. User Details Manager: Fetches user details and roles from the database.
    @Bean
    public JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);

        // Customize the query to fetch user details from your existing "user" table
        manager.setUsersByUsernameQuery("""
            SELECT username, password, enabled
            FROM "user"
            WHERE username = ?
        """);

        // Customize the query to fetch authorities/roles from your "authorities" table
        manager.setAuthoritiesByUsernameQuery("""
            SELECT username, authority
            FROM authorities
            WHERE username = ?
        """);

        return manager;
    }

    // 3. Filter Chain: Defines HTTP access rules.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/users/**")
                )
                .authorizeHttpRequests(auth -> auth
                        // Authorization rules remain the same
                        .requestMatchers(HttpMethod.PUT, "/api/users/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}