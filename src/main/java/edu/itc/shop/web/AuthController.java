
package edu.itc.shop.web;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.shop.config.SecurityProps;
import edu.itc.shop.service.LoginRateLimiter;
import edu.itc.shop.service.RefreshTokenService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtEncoder encoder;
    private final SecurityProps props;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(
            AuthenticationManager authManager,
            JwtEncoder encoder,
            SecurityProps props,
            RefreshTokenService refreshTokenService,
            UserDetailsService userDetailsService,
            LoginRateLimiter loginRateLimiter) {

        this.authManager = authManager;
        this.encoder = encoder;
        this.props = props;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid LoginRequest req,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();

        long retryAfter = loginRateLimiter.tryAcquire(
                ip, req.username());

        if (retryAfter > 0) {

            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Please try again later.");

            pd.setTitle("Too Many Requests");

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER,
                            String.valueOf(retryAfter))
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(pd);
        }

        Authentication auth = authManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        req.username(), req.password()));

        TokenResponse access = issueTokens(
                auth.getName(), auth.getAuthorities());

        RefreshTokenService.IssuedRefreshToken refresh =
                refreshTokenService.create(auth.getName());

        long refreshExpiresIn = Math.max(0,
                Duration.between(
                        Instant.now(), refresh.expiresAt()).toSeconds());

        TokenResponse response = new TokenResponse(
                access.token(),
                access.tokenType(),
                access.expiresIn(),
                refresh.token(),
                refreshExpiresIn
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody @Valid RefreshRequest req) {

        RefreshTokenService.RotationResult result =
                refreshTokenService.rotate(req.refreshToken());

        if (result.status()
                == RefreshTokenService.RotationStatus.REUSE_DETECTED) {

            return problem(HttpStatus.UNAUTHORIZED,
                    "Refresh token reuse detected");
        }

        if (result.status()
                != RefreshTokenService.RotationStatus.SUCCESS) {

            return problem(HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token");
        }

        UserDetails user =
                userDetailsService.loadUserByUsername(result.username());

        TokenResponse response = issueTokens(
                user.getUsername(), user.getAuthorities());

        var rotated = result.refreshToken();

        return ResponseEntity.ok(new TokenResponse(
                response.token(),
                response.tokenType(),
                response.expiresIn(),
                rotated.token(),
                Math.max(0, Duration.between(
                        Instant.now(), rotated.expiresAt()).toSeconds())
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody @Valid LogoutRequest req) {

        refreshTokenService.logout(req.refreshToken());

        return ResponseEntity.noContent().build();
    }

    private TokenResponse issueTokens(
            String username,
            java.util.Collection<? extends GrantedAuthority> authorities) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("shop-api")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("roles", authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .build();

        JwsHeader header =
                JwsHeader.with(MacAlgorithm.HS256).build();

        String accessToken = encoder.encode(
                JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new TokenResponse(
                accessToken,
                "Bearer",
                props.accessTokenTtl().toSeconds(),
                null,
                0
        );
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String detail) {

        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(status, detail);

        pd.setTitle(status.getReasonPhrase());

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> badCredentials() {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Bad credentials");

        pd.setTitle("Unauthorized");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
