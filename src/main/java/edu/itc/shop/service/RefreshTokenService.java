
package edu.itc.shop.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import edu.itc.shop.domain.RefreshToken;
import edu.itc.shop.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long REFRESH_TOKEN_DAYS = 7;

    private final RefreshTokenRepository repository;
    private final AuditService auditService;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public IssuedRefreshToken create(String username) {
        String familyId = UUID.randomUUID().toString();
        return createInFamily(username, familyId, Instant.now());
    }

    @Transactional
    public RotationResult rotate(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return new RotationResult(RotationStatus.INVALID, null, null);
        }

        String tokenHash = hash(rawToken);

        var optionalToken =
                repository.findByTokenHashForUpdate(tokenHash);

        if (optionalToken.isEmpty()) {
            return new RotationResult(RotationStatus.INVALID, null, null);
        }

        RefreshToken oldToken = optionalToken.get();
        Instant now = Instant.now();

        // Detect reuse and revoke the entire token family.
        if (oldToken.isRevoked()) {
            revokeFamily(oldToken.getFamilyId(), now);

            auditService.record(
                    "REFRESH_REUSE_DETECTED",
                    oldToken.getUsername(),
                    getClientIp(),
                    MDC.get("rid"),
                    "Revoked refresh token reuse detected"
            );

            return new RotationResult(
                    RotationStatus.REUSE_DETECTED,
                    null,
                    oldToken.getUsername()
            );
        }

        // Reject expired tokens.
        if (!oldToken.getExpiresAt().isAfter(now)) {
            oldToken.revoke(now);

            return new RotationResult(
                    RotationStatus.INVALID,
                    null,
                    oldToken.getUsername()
            );
        }

        // Create a replacement in the same family.
        IssuedRefreshToken replacement = createInFamily(
                oldToken.getUsername(),
                oldToken.getFamilyId(),
                now
        );

        oldToken.revoke(now);
        oldToken.setReplacedBy(hash(replacement.token()));

        auditService.record(
                "TOKEN_REFRESHED",
                oldToken.getUsername(),
                getClientIp(),
                MDC.get("rid"),
                "Refresh token successfully rotated"
        );

        return new RotationResult(
                RotationStatus.SUCCESS,
                replacement,
                oldToken.getUsername()
        );
    }

    @Transactional
    public boolean logout(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String tokenHash = hash(rawToken);

        var optionalToken =
                repository.findByTokenHashForUpdate(tokenHash);

        if (optionalToken.isEmpty()) {
            return false;
        }

        RefreshToken token = optionalToken.get();

        revokeFamily(token.getFamilyId(), Instant.now());

        auditService.record(
                "LOGOUT",
                token.getUsername(),
                getClientIp(),
                MDC.get("rid"),
                "User logged out and refresh-token family revoked"
        );

        return true;
    }

    private void revokeFamily(String familyId, Instant now) {
        for (RefreshToken token : repository.findAllByFamilyId(familyId)) {
            token.revoke(now);
        }
    }

    private IssuedRefreshToken createInFamily(
            String username,
            String familyId,
            Instant now) {

        byte[] randomBytes = new byte[32];
        RANDOM.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = hash(rawToken);

        Instant expiresAt = now.plus(
                REFRESH_TOKEN_DAYS,
                ChronoUnit.DAYS
        );

        RefreshToken refreshToken = new RefreshToken(
                tokenHash,
                username,
                familyId,
                expiresAt
        );

        repository.save(refreshToken);

        return new IssuedRefreshToken(
                rawToken,
                familyId,
                expiresAt
        );
    }

    private String getClientIp() {
        var attributes = RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }

        return null;
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashedBytes = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hashedBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
    }

    public enum RotationStatus {
        SUCCESS,
        INVALID,
        REUSE_DETECTED
    }

    public record IssuedRefreshToken(
            String token,
            String familyId,
            Instant expiresAt
    ) {}

    public record RotationResult(
            RotationStatus status,
            IssuedRefreshToken refreshToken,
            String username
    ) {}
}
