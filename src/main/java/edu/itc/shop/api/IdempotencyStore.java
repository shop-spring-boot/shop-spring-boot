package edu.itc.shop.api;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.ws.rs.core.Link;

/** First response per Idempotency-Key. One bound singleton, shared by all requests. */
public class IdempotencyStore {

    /** What is replayed: the stored status, Location, Link headers and body, plus a hash of the original request. */
    public record Stored(String bodyHash, int status, URI location, List<Link> links, Object body) { }

    private final ConcurrentHashMap<String, Stored> entries = new ConcurrentHashMap<>();

    /** Atomic per key: two concurrent retries run the creation function only once. */
    public Stored computeIfAbsent(String key, Function<String, Stored> create) {
        return entries.computeIfAbsent(key, create);
    }
}
