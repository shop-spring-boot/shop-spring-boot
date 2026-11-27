package edu.itc.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Task 1: our first SecurityFilterChain.
 * Task 2: permit /api/register and use JpaUserDetailsService from the database.
 * Task 4: HTTP Basic replaced by JWT. /api/auth/login issues an HS256 token and every
 *         other request is validated as an OAuth2 resource server (Bearer token, STATELESS).
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtAuthenticationConverter,
                                            ProblemAuthenticationEntryPoint entryPoint) throws Exception {
        http
            // Public endpoints: no login needed. Everything else: must be authenticated.
            .authorizeHttpRequests(auth -> auth
                // TASK 4: login endpoint is public (it is how you get a token)
                .requestMatchers("/api/auth/**", "/api/public/**", "/api/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/*/ship").hasRole("ADMIN")
                .anyRequest().authenticated())

            // TASK 4: removed .httpBasic(...). The API now validates a JWT on every request.
            // The decoder checks signature (HS256), expiry and issuer "shop-api";
            // the converter maps the "roles" claim to authorities (empty prefix).
            .oauth2ResourceServer(o -> o
                .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter))
                // TASK 4: expired/invalid tokens -> 401 problem+json + WWW-Authenticate: Bearer
                .authenticationEntryPoint(entryPoint))

            // TASK 4: same entry point for requests with no token at all
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))

            // TASK 4: no HTTP session and no JSESSIONID cookie; the token carries the identity
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CSRF disabled: Bearer only. The token is sent in the Authorization header,
            // never in a cookie, so a browser cannot attach it automatically (no CSRF risk).
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}