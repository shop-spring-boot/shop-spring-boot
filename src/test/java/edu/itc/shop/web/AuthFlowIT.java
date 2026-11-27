package edu.itc.shop.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef")
class AuthFlowIT {

    @Autowired TestRestTemplate rest;
    @Autowired JwtEncoder encoder;

    private HttpEntity<Void> bearer(String t) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(t);
        return new HttpEntity<>(h);
    }

    @Test
    void loginThenAccessThenExpired() {
        var login = rest.postForEntity("/api/auth/login",
                new LoginRequest("alice", "Alice-pass-123"), TokenResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = login.getBody().token();

        assertThat(rest.exchange("/api/orders", HttpMethod.GET, bearer(token), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        Instant past = Instant.now().minusSeconds(3600);
        var claims = JwtClaimsSet.builder().issuer("shop-api").subject("alice")
                .issuedAt(past.minusSeconds(900)).expiresAt(past)
                .claim("roles", List.of("ROLE_CUSTOMER")).build();
        String expired = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        var res = rest.exchange("/api/orders", HttpMethod.GET, bearer(expired), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat((String) res.getBody().get("detail")).containsIgnoringCase("expired");
    }
}