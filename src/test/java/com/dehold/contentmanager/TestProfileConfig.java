package com.dehold.contentmanager;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@Profile("test")
public class TestProfileConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/**"))
                .authorizeHttpRequests(auth -> auth

                        // === REAL SECURITY REPRODUCED ===

                        // Admin APIs (for RateLimit tests)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Webhooks MUST BE PUBLIC (they are callbacks)
                        .requestMatchers("/api/users/*/webhooks/**").permitAll()

                        // Production-like rules
                        .requestMatchers(HttpMethod.PUT, "/api/users/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").authenticated()

                        // Everything else open
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
    @Bean @Primary
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password("pass").roles("ADMIN").build(),
                User.withUsername("user").password("pass").roles("USER").build()
        );
    }


    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
         return NoOpPasswordEncoder.getInstance();
    }
}
