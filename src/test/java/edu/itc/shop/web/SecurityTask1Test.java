package edu.itc.shop.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTask1Test {

    @Autowired
    MockMvc mvc;

    @Test
    void publicEndpointNeedsNoLogin() throws Exception {
        mvc.perform(get("/api/public/hello")).andExpect(status().isOk());
    }

    @Test
    void privateEndpointWithoutLoginIs401() throws Exception {
        mvc.perform(get("/api/private/hello")).andExpect(status().isUnauthorized());
    }

    @Test
    void privateEndpointWithWrongPasswordIs401() throws Exception {
        mvc.perform(get("/api/private/hello").with(httpBasic("alice", "wrong")))
            .andExpect(status().isUnauthorized());
    }

}