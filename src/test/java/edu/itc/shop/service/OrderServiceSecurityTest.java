package edu.itc.shop.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import edu.itc.shop.domain.Order;
import edu.itc.shop.repository.OrderRepository;

@SpringBootTest
class OrderServiceSecurityTest {

    @Autowired
    OrderService service;

    @MockitoBean
    OrderRepository orders;

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void aliceCanReadHerOwnHistory() {
        List<Order> expected = List.of(new Order("alice", 2L, 1));
        when(orders.findByCustomer("alice")).thenReturn(expected);

        List<Order> result = service.history("alice");

        assertThat(result).hasSize(1);
        verify(orders).findByCustomer("alice");
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void aliceCannotReadBobsHistory() {
        assertThrows(AccessDeniedException.class, () -> service.history("bob"));
        verifyNoInteractions(orders);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanReadBobsHistory() {
        List<Order> expected = List.of(new Order("bob", 3L, 2));
        when(orders.findByCustomer("bob")).thenReturn(expected);

        List<Order> result = service.history("bob");

        assertThat(result).hasSize(1);
        verify(orders).findByCustomer("bob");
    }

    @Test
    @WithMockUser(username = "alice", roles = "CUSTOMER")
    void aliceCannotShipOrder() {
        assertThrows(AccessDeniedException.class, () -> service.ship(1L));
        verifyNoInteractions(orders);
    }

    @Test
    void unauthenticatedUserIsRejected() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> service.history("alice"));
        verifyNoInteractions(orders);
    }
}