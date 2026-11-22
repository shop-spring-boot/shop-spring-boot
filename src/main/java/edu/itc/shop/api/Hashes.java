package edu.itc.shop.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/** SHA-256 of the JSON form of a request body, used to detect an Idempotency-Key reused with another body. */
final class Hashes {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private Hashes() { }

    static String sha256(Object body) {
        try {
            byte[] json = JSONB.toJson(body).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);   // every JVM has SHA-256
        }
    }
}
