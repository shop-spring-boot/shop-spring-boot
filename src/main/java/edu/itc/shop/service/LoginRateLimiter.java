
package edu.itc.shop.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_NANOS = 60_000_000_000L;

    private final Map<String, Window> attempts = new HashMap<>();

    /**
     * Returns 0 when allowed.
     * Returns retry seconds when the limit is reached.
     */
    public synchronized long tryAcquire(String ip, String username) {

        long now = System.nanoTime();

        String ipKey = "IP:" + ip;
        String usernameKey = "USER:" + username.trim().toLowerCase();

        attempts.entrySet().removeIf(
                entry -> now - entry.getValue().startedAt >= WINDOW_NANOS);

        Window ipWindow = attempts.get(ipKey);
        Window usernameWindow = attempts.get(usernameKey);

        long ipWait = getRetryAfter(ipWindow, now);
        long usernameWait = getRetryAfter(usernameWindow, now);

        if (ipWait > 0 || usernameWait > 0) {
            return Math.max(ipWait, usernameWait);
        }

        recordAttempt(ipKey, now);
        recordAttempt(usernameKey, now);

        return 0;
    }

    private long getRetryAfter(Window window, long now) {

        if (window == null || window.count < MAX_ATTEMPTS) {
            return 0;
        }

        long remaining = WINDOW_NANOS - (now - window.startedAt);

        return Math.max(1,
                (remaining + 999_999_999L) / 1_000_000_000L);
    }

    private void recordAttempt(String key, long now) {

        Window window = attempts.get(key);

        if (window == null) {
            attempts.put(key, new Window(now));
        } else {
            window.count++;
        }
    }

    /**
     * Clears all rate-limit records.
     * Useful for isolated integration tests.
     */
    public synchronized void reset() {
        attempts.clear();
    }

    private static class Window {

        private final long startedAt;
        private int count = 1;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
