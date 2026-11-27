
package edu.itc.shop.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    @Test
    void allowsFiveAttemptsAndBlocksSixth() {

        // Create a fresh limiter for this test.
        LoginRateLimiter limiter = new LoginRateLimiter();

        String ip = "192.0.2.10";
        String username = "rate-test-user";

        // First five attempts should be allowed.
        for (int i = 1; i <= 5; i++) {
            assertThat(limiter.tryAcquire(ip, username))
                    .as("Attempt %s should be allowed", i)
                    .isZero();
        }

        // Sixth attempt should be blocked.
        long retryAfter = limiter.tryAcquire(ip, username);

        assertThat(retryAfter).isGreaterThan(0);
    }

    @Test
    void limitsAttemptsPerIpAcrossDifferentUsernames() {

        LoginRateLimiter limiter = new LoginRateLimiter();

        String ip = "192.0.2.20";

        for (int i = 1; i <= 5; i++) {
            assertThat(limiter.tryAcquire(ip, "user-" + i))
                    .isZero();
        }

        // Same IP, different username: still blocked.
        assertThat(limiter.tryAcquire(ip, "another-user"))
                .isGreaterThan(0);
    }

    @Test
    void limitsAttemptsPerUsernameAcrossDifferentIps() {

        LoginRateLimiter limiter = new LoginRateLimiter();

        String username = "shared-test-user";

        for (int i = 1; i <= 5; i++) {
            assertThat(limiter.tryAcquire(
                    "192.0.2." + i, username))
                    .isZero();
        }

        // Same username, different IP: still blocked.
        assertThat(limiter.tryAcquire(
                "192.0.2.99", username))
                .isGreaterThan(0);
    }
}
