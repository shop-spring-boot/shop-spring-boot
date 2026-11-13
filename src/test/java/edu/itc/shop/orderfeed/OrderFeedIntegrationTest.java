package edu.itc.shop.orderfeed;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import edu.itc.shop.domain.Address;
import edu.itc.shop.domain.Customer;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.level.org.hibernate.SQL=DEBUG",
                "logging.level.org.hibernate.orm.jdbc.bind=OFF"
        })
class OrderFeedIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${local.server.port}")
    int port;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> insertOrders());
    }

    @Test
    void sliceFeedContinuesByPlacedAtAndIdWithoutSkippingOrRepeating() throws Exception {
        assertFeed("/api/orders/feed");
    }

    @Test
    void keysetFeedContinuesWithAWindowAndKeysetPosition() throws Exception {
        assertFeed("/api/orders/feed/keyset");
    }

    private void assertFeed(String endpoint) throws Exception {
        List<Long> ids = new ArrayList<>();
        String cursor = null;
        boolean hasNext = true;
        int requests = 0;
        while (hasNext) {
            String url = endpoint + "?size=2"
                    + (cursor == null ? "" : "&after=" + cursor);
            JsonNode response = objectMapper.readTree(client.get().uri(URI.create(url))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody());
            JsonNode items = response.get("items");
            for (JsonNode item : items) {
                ids.add(item.get("id").asLong());
            }
            hasNext = response.get("hasNext").asBoolean();
            cursor = response.hasNonNull("nextCursor") ? response.get("nextCursor").asText() : null;
            assertThat(cursor != null).isEqualTo(hasNext);
            requests++;
        }

        assertThat(ids).hasSize(5).doesNotHaveDuplicates().isSorted();
        assertThat(requests).isEqualTo(3);
    }

    private void insertOrders() {
        entityManager.createQuery("delete from Order").executeUpdate();
        Address address = new Address("1 Report Way", "Phnom Penh", "12000");
        Customer customer = new Customer("Feed Customer", "feed-" + System.nanoTime() + "@example.test",
                address, address);
        entityManager.persist(customer);
        Instant sameTimestamp = Instant.parse("2026-09-20T08:11:02.331Z");
        for (int i = 0; i < 5; i++) {
            entityManager.persist(new Order(customer, OrderStatus.NEW, sameTimestamp));
        }
        entityManager.flush();
    }
}
