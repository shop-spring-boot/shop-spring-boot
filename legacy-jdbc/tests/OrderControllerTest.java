package edu.itc.shop.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.itc.shop.audit.RequestContext;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(RequestContext.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OrderControllerTest {

    private static final String VALID_BODY =
            "{\"customerId\":1,\"lines\":[{\"productId\":1,\"qty\":2}]}";

    private final MockMvc mvc;

    @MockitoBean
    private OrderService service;

    OrderControllerTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void placeReturns201WithLocation() throws Exception {
        when(service.place(any())).thenReturn(new OrderDto(17, OrderStatus.NEW,
                new BigDecimal("131.88"),
                List.of(new OrderLine(1, 2, new BigDecimal("49.99")))));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/17"))
                .andExpect(jsonPath("$.id").value(17))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void placeWithNoLinesReturns400() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"lines\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void placeWithZeroQuantityReturns400() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"lines\":[{\"productId\":1,\"qty\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insufficientStockReturns409() throws Exception {
        when(service.place(any())).thenThrow(new InsufficientStockException(2));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Insufficient stock for product 2"));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        when(service.get(999L)).thenThrow(new OrderNotFoundException(999));

        mvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancellingANonNewOrderReturns409() throws Exception {
        when(service.cancel(1L)).thenThrow(new OrderNotCancellableException(1, OrderStatus.CANCELLED));

        mvc.perform(post("/api/orders/1/cancel"))
                .andExpect(status().isConflict());
    }

    @Test
    void listReturnsPage() throws Exception {
        when(service.list(1L, OrderStatus.NEW, 0, 10))
                .thenReturn(new Page<>(List.of(), 0, 10, 0));

        mvc.perform(get("/api/orders?customerId=1&status=NEW&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void listWithInvalidSizeReturns400() throws Exception {
        mvc.perform(get("/api/orders?size=0"))
                .andExpect(status().isBadRequest());
    }
}