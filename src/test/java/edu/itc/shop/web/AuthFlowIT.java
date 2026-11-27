
package edu.itc.shop.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.TestPropertySource;

import edu.itc.shop.domain.UserAccount;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.service.LoginRateLimiter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef")
class AuthFlowIT {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JwtEncoder encoder;

    @Autowired
    LoginRateLimiter loginRateLimiter;

    @Autowired
    UserAccountRepository userAccountRepository;

    @BeforeEach
    void resetRateLimiter() {
        loginRateLimiter.reset();

        userAccountRepository.findByUsername("alice").ifPresent(account -> {
            account.resetFailedLogins();
            userAccountRepository.save(account);
        });
    }

    @AfterEach
    void cleanupAccountLockout() {
        userAccountRepository.findByUsername("alice").ifPresent(account -> {
            account.resetFailedLogins();
            userAccountRepository.save(account);
        });

        loginRateLimiter.reset();
    }

    private HttpEntity<Void> bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void loginThenAccessThenExpired() {

        var login = rest.postForEntity("/api/auth/login",
                new LoginRequest("alice", "Alice-pass-123"),
                TokenResponse.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();

        String token = login.getBody().token();

        assertThat(rest.exchange("/api/orders", HttpMethod.GET,
                bearer(token), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        Instant past = Instant.now().minusSeconds(3600);

        var claims = JwtClaimsSet.builder()
                .issuer("shop-api")
                .subject("alice")
                .issuedAt(past.minusSeconds(900))
                .expiresAt(past)
                .claim("roles", List.of("ROLE_CUSTOMER"))
                .build();

        String expired = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        var response = rest.exchange("/api/orders", HttpMethod.GET,
                bearer(expired), Map.class);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat((String) response.getBody().get("detail"))
                .containsIgnoringCase("expired");
    }

    @Test
    void refreshRotationAndReuseRevokesEntireFamily() {

        var login = rest.postForEntity("/api/auth/login",
                new LoginRequest("alice", "Alice-pass-123"),
                TokenResponse.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();

        String oldRefreshToken = login.getBody().refreshToken();

        var refresh = rest.postForEntity("/api/auth/refresh",
                new RefreshRequest(oldRefreshToken),
                TokenResponse.class);

        assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refresh.getBody()).isNotNull();

        String newRefreshToken = refresh.getBody().refreshToken();

        assertThat(newRefreshToken).isNotBlank();
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        assertThat(rest.exchange("/api/orders", HttpMethod.GET,
                bearer(refresh.getBody().token()), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        var reuse = rest.postForEntity("/api/auth/refresh",
                new RefreshRequest(oldRefreshToken),
                Map.class);

        assertThat(reuse.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        var newestRefresh = rest.postForEntity("/api/auth/refresh",
                new RefreshRequest(newRefreshToken),
                Map.class);

        assertThat(newestRefresh.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesRefreshTokenFamily() {

        var login = rest.postForEntity("/api/auth/login",
                new LoginRequest("alice", "Alice-pass-123"),
                TokenResponse.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();

        String refreshToken = login.getBody().refreshToken();

        var logout = rest.postForEntity("/api/auth/logout",
                new LogoutRequest(refreshToken),
                Void.class);

        assertThat(logout.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        var refresh = rest.postForEntity("/api/auth/refresh",
                new RefreshRequest(refreshToken),
                Map.class);

        assertThat(refresh.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginRateLimitBlocksSixthAttempt() {

        for (int i = 1; i <= 5; i++) {

            var response = rest.postForEntity(
                    "/api/auth/login",
                    new LoginRequest("rate-test-user", "Wrong-password"),
                    Map.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        var blocked = rest.postForEntity(
                "/api/auth/login",
                new LoginRequest("rate-test-user", "Wrong-password"),
                Map.class);

        assertThat(blocked.getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        assertThat(blocked.getHeaders().getFirst("Retry-After"))
                .isNotBlank();
    }

    @Test
    void accountLocksAfterFiveFailedLoginAttempts() {

        for (int i = 1; i <= 5; i++) {

            var response = rest.postForEntity(
                    "/api/auth/login",
                    new LoginRequest("alice", "Wrong-password"),
                    Map.class);

            assertThat(response.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            // Isolate account lockout from IP rate limiting.
            loginRateLimiter.reset();
        }

        UserAccount account = userAccountRepository
                .findByUsername("alice")
                .orElseThrow();

        assertThat(account.getFailedAttempts()).isEqualTo(5);
        assertThat(account.getLockedUntil()).isNotNull();
        assertThat(account.isAccountNonLocked()).isFalse();

        // Correct password must also be rejected while locked.
        var correctLogin = rest.postForEntity(
                "/api/auth/login",
                new LoginRequest("alice", "Alice-pass-123"),
                Map.class);

        assertThat(correctLogin.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
