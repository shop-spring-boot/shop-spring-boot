package edu.itc.shop.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WelcomeServiceTest {

    static class FakeNotifier implements Notifier {
        final List<String> calls = new ArrayList<>();

        @Override
        public void send(String to, String text) {
            calls.add(to + "|" + text);
        }
    }

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-25T09:15:02Z"), ZoneOffset.UTC);

    @Test
    void normalWelcomeUsesDefaultNotifier() {
        var normal = new FakeNotifier();
        var urgent = new FakeNotifier();
        var service = new WelcomeService(normal, urgent, clock);

        String text = service.Welcome("ann", false);

        assertEquals("Welcome ann! It is 09:15:02", text);
        assertEquals(List.of("ann|Welcome ann! It is 09:15:02"), normal.calls);
        assertEquals(0, urgent.calls.size());
    }

    @Test
    void urgentWelcomeUsesUrgentNotifier() {
        var normal = new FakeNotifier();
        var urgent = new FakeNotifier();
        var service = new WelcomeService(normal, urgent, clock);

        service.Welcome("bob", true);

        assertEquals(0, normal.calls.size());
        assertEquals(1, urgent.calls.size());
    }
}