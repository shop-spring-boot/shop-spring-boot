package edu.itc.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Task 1: our first SecurityFilterChain.
 * Task 2: permit /api/register and use JpaUserDetailsService from the database.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Public endpoints: no login needed. Everything else: must be authenticated.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**", "/api/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/*/ship").hasRole("ADMIN")
                .anyRequest().authenticated())
            // HTTP Basic: the client sends "Authorization: Basic base64(user:password)".
            .httpBasic(Customizer.withDefaults())
            // CSRF disabled: this chain accepts no cookie-based credentials (Basic / Bearer only)
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}