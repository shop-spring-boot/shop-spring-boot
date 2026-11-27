package edu.itc.shop.web;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.itc.shop.config.SecurityConfig;
import edu.itc.shop.repository.UserAccountRepository;
import edu.itc.shop.service.AccountService;
import edu.itc.shop.service.OrderService;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService orders;

    @MockitoBean
    UserAccountRepository users;

    @MockitoBean
    AccountService accounts;

    @Test
    void anonymousCannotShipOrder() throws Exception {
        mvc.perform(post("/api/orders/1/ship"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orders);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotShipOrder() throws Exception {
        mvc.perform(post("/api/orders/1/ship"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orders);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanShipOrder() throws Exception {
        mvc.perform(post("/api/orders/1/ship"))
                .andExpect(status().isNoContent());
        verify(orders).ship(1L);
    }
}