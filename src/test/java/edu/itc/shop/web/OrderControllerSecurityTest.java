package edu.itc.shop.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.itc.shop.config.ProblemAuthenticationEntryPoint;
import edu.itc.shop.config.SecurityConfig;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.service.AccountService;
import edu.itc.shop.service.OrderService;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, ProblemAuthenticationEntryPoint.class})
class OrderControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService orders;

    @MockitoBean
    UserAccountRepository users;

    @MockitoBean
    AccountService accounts;

    // Task 4: beans that SecurityConfig needs (normally from JwtConfig).
    // jwt() bypasses real validation, so mocks are enough here.
    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void anonymousCannotShipOrder() throws Exception {
        mvc.perform(post("/api/orders/1/ship"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orders);
    }

    @Test
    void customerCannotShipOrder() throws Exception {
        mvc.perform(post("/api/orders/1/ship").with(jwt()
                        .jwt(j -> j.subject("alice").claim("roles", List.of("ROLE_CUSTOMER")))
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orders);
    }

    @Test
    void adminCanShipOrder() throws Exception {
        mvc.perform(post("/api/orders/1/ship").with(jwt()
                        .jwt(j -> j.subject("admin").claim("roles", List.of("ROLE_ADMIN")))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
        verify(orders).ship(1L);
    }

    @Test
    void customerWithJwtCanListOrders() throws Exception {
        mvc.perform(get("/api/orders").with(jwt()
                        .jwt(j -> j.subject("alice").claim("roles", List.of("ROLE_CUSTOMER")))
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
        verify(orders).history("alice");
    }

    @Test
    void garbageTokenIs401() throws Exception {
        when(jwtDecoder.decode("not.a.jwt")).thenThrow(new BadJwtException("Malformed token"));

        mvc.perform(get("/api/orders").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        verifyNoInteractions(orders);
    }
}