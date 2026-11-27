
package edu.itc.shop.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef")
class AuditAccessIT {

    @Autowired
    TestRestTemplate rest;

    private TokenResponse login(String username, String password) {
        var response = rest.postForEntity(
                "/api/auth/login",
                new LoginRequest(username, password),
                TokenResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    private HttpEntity<Void> bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    @Test
    void adminCanReadAuditLogs() {

        TokenResponse admin = login("admin", "Admin-pass-123");

        ResponseEntity<List> response = rest.exchange(
                "/api/admin/audit",
                HttpMethod.GET,
                bearer(admin.token()),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void customerCannotReadAuditLogs() {

        TokenResponse customer = login("alice", "Alice-pass-123");

        ResponseEntity<Map> response = rest.exchange(
                "/api/admin/audit",
                HttpMethod.GET,
                bearer(customer.token()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
