package edu.itc.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Task 1: our first SecurityFilterChain.
 *
 * Without this class Spring Boot protects EVERY URL with a generated password
 * (user = "user", password printed in the console at startup).
 * Here we decide ourselves which URLs are public and which need a login.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Public endpoints: no login needed. Everything else: must be authenticated.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated())
            // HTTP Basic: the client sends "Authorization: Basic base64(user:password)".
            .httpBasic(Customizer.withDefaults())
            // CSRF is disabled for now so curl POST works.
            // We will decide the real CSRF strategy in Task 5.
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /** BCrypt: the password is stored as a salted hash, never as plain text. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Temporary in-memory user, only for Task 1.
     * Task 2 replaces this with users loaded from the database.
     */
    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
            User.withUsername("user")
                .password(encoder.encode("password"))
                .roles("USER")
                .build());
    }
}
