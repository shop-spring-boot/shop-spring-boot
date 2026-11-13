package edu.itc.shop.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import edu.itc.shop.domain.Category;
import edu.itc.shop.domain.Money;
import edu.itc.shop.domain.Product;
import edu.itc.shop.domain.Tag;
import edu.itc.shop.persistence.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
class ProductReportIntegrationTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProductRepository products;

    @org.springframework.beans.factory.annotation.Value("${local.server.port}")
    int port;

    private RestTestClient client;
    private String prefix;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        prefix = "RP" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> insertProducts());
    }

    @Test
    void appliesEachFilterCombinedFiltersSortingAndEmptyResults() throws Exception {
        assertThat(products.findBySku(prefix + "-A")).isPresent();
        assertThat(products.countBySkuStartingWith(prefix)).isEqualTo(3);
        assertThat(report("")).isNotEmpty();
        assertThat(report("q=" + prefix)).hasSize(3);
        assertThat(report("category=HARDWARE&q=" + prefix)).hasSize(2);
        assertThat(report("maxPrice=25&q=" + prefix)).extracting(ProductReportRow::sku)
                .containsExactly(prefix + "-B");
        assertThat(report("minStock=8&q=" + prefix)).extracting(ProductReportRow::sku)
                .containsExactly(prefix + "-C");
        assertThat(report("tag=" + prefix.toLowerCase(java.util.Locale.ROOT) + "-sale&q=" + prefix))
                .extracting(ProductReportRow::sku)
                .containsExactlyInAnyOrder(prefix + "-A", prefix + "-C");

        List<ProductReportRow> combined = report("q=" + prefix
                + "&category=HARDWARE&maxPrice=130&minStock=1&tag="
                + prefix.toLowerCase(java.util.Locale.ROOT) + "-sale");
        assertThat(combined).containsExactly(new ProductReportRow(
                prefix + "-A", "Report " + prefix + " alpha", new BigDecimal("129.00"), 4, 2));
        assertThat(report("q=" + prefix + "&sort=amount,desc"))
                .extracting(ProductReportRow::sku)
                .containsExactly(prefix + "-C", prefix + "-A", prefix + "-B");
        assertThat(report("q=missing-" + prefix)).isEmpty();
    }

    @Test
    void rejectsSortValuesOutsideTheWhitelist() {
        client.get().uri("/api/reports/products?sort=name desc;drop table product")
                .exchange()
                .expectStatus().isBadRequest();
    }

    private List<ProductReportRow> report(String query) throws Exception {
        String json = client.get().uri("/api/reports/products?" + query)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        return objectMapper.readValue(json, new TypeReference<>() { });
    }

    private void insertProducts() {
        Tag sale = new Tag(prefix + "-sale");
        Tag featured = new Tag(prefix + "-featured");
        Tag clearance = new Tag(prefix + "-clearance");
        entityManager.persist(sale);
        entityManager.persist(featured);
        entityManager.persist(clearance);

        Product first = new Product(prefix + "-A", "Report " + prefix + " alpha",
                Category.HARDWARE, new Money(new BigDecimal("129.00"), "USD"), 4);
        first.tag(sale);
        first.tag(featured);
        Product second = new Product(prefix + "-B", "Report " + prefix + " beta",
                Category.HARDWARE, new Money(new BigDecimal("20.00"), "USD"), 0);
        second.tag(clearance);
        Product third = new Product(prefix + "-C", "Report " + prefix + " gamma",
                Category.FOOD, new Money(new BigDecimal("200.00"), "USD"), 8);
        third.tag(sale);
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.persist(third);
        entityManager.flush();
    }
}
