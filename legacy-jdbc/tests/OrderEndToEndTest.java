package edu.itc.shop.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql("/reset.sql")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OrderEndToEndTest {

    private static final String PLACE_OK =
            "{\"customerId\":1,\"lines\":[{\"productId\":1,\"qty\":2},{\"productId\":3,\"qty\":1}]}";
    private static final String PLACE_TOO_MUCH =
            "{\"customerId\":1,\"lines\":[{\"productId\":1,\"qty\":2},{\"productId\":2,\"qty\":999}]}";
    private static final String PLACE_ONE_CABLE =
            "{\"customerId\":1,\"lines\":[{\"productId\":3,\"qty\":1}]}";

    private final RestTestClient client;
    private final JdbcClient jdbc;

    OrderEndToEndTest(@Value("${local.server.port}") int port, JdbcClient jdbc) {
        this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        this.jdbc = jdbc;
    }

    private int stockOf(long productId) {
        return jdbc.sql("SELECT stock FROM product WHERE id = :id")
                .param("id", productId).query(Integer.class).single();
    }

    private long orderCount() {
        return jdbc.sql("SELECT COUNT(*) FROM orders").query(Long.class).single();
    }

    @Test
    void placeThenCancelRestoresStock() {
        OrderDto placed = client.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON).body(PLACE_OK)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderDto.class).returnResult().getResponseBody();

        assertThat(placed.status()).isEqualTo(OrderStatus.NEW);
        assertThat(placed.total()).isEqualByComparingTo("120.98");
        assertThat(stockOf(1)).isEqualTo(8);
        assertThat(stockOf(3)).isEqualTo(19);

        client.post().uri("/api/orders/{id}/cancel", placed.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("CANCELLED");

        assertThat(stockOf(1)).isEqualTo(10);
        assertThat(stockOf(3)).isEqualTo(20);
    }

    @Test
    void failedOrderRollsBackEverything() {
        client.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON).body(PLACE_TOO_MUCH)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.detail").isEqualTo("Insufficient stock for product 2");

        assertThat(stockOf(1)).isEqualTo(10);
        assertThat(stockOf(2)).isEqualTo(5);
        assertThat(orderCount()).isZero();
    }

    @Test
    void cancellingTwiceReturns409() {
        OrderDto placed = client.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON).body(PLACE_OK)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OrderDto.class).returnResult().getResponseBody();

        client.post().uri("/api/orders/{id}/cancel", placed.id()).exchange().expectStatus().isOk();
        client.post().uri("/api/orders/{id}/cancel", placed.id()).exchange().expectStatus().isEqualTo(409);

        assertThat(stockOf(1)).isEqualTo(10);
    }

    @Test
    void listPaginates() {
        for (int i = 0; i < 3; i++) {
            client.post().uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON).body(PLACE_ONE_CABLE)
                    .exchange().expectStatus().isCreated();
        }

        client.get().uri("/api/orders?customerId=1&page=0&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(3)
                .jsonPath("$.content.length()").isEqualTo(2);
    }
}