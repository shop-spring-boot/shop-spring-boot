
package edu.itc.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import edu.itc.shop.security.RequestIdFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ProblemAuthenticationEntryPoint entryPoint) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                        "/api/auth/**",
                        "/api/public/**",
                        "/api/register"
                ).permitAll()

                .requestMatchers(
                        HttpMethod.GET,
                        "/products/*/reviews.html",
                        "/api/products/*/reviews"
                ).permitAll()

                // Admin-only endpoints
                .requestMatchers(
                        HttpMethod.POST,
                        "/api/orders/*/ship"
                ).hasRole("ADMIN")

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/admin/audit"
                ).hasRole("ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            .oauth2ResourceServer(o -> o
                .jwt(j -> j.jwtAuthenticationConverter(
                        jwtAuthenticationConverter))
                .authenticationEntryPoint(entryPoint)
            )

            .exceptionHandling(e ->
                e.authenticationEntryPoint(entryPoint)
            )

            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .csrf(AbstractHttpConfigurer::disable)

            .headers(h -> h
                .frameOptions(f -> f.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000)
                    .requestMatcher(request -> true)
                )
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; frame-ancestors 'none'; object-src 'none'"
                ))
                .referrerPolicy(r -> r.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy
                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                ))
            )

            .addFilterBefore(
                new RequestIdFilter(),
                AuthorizationFilter.class
            );

        return http.build();
    }
}
