package edu.itc.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * One test per status code the API promises (200, 201, 204, 304, 400, 401, 404, 406, 409, 412, 415, 422, 428),
 * plus the order flow, idempotency and the OpenAPI document. Every test creates its own data, so the
 * tests do not depend on each other or on the order they run in.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class ShopApiTest extends JerseyTest {

    static {
        System.setProperty("jersey.config.test.container.port", "0");   // any free port
    }

    private static final String KEY = "demo-123";
    private static final String PROBLEM = "application/problem+json";
    private static final AtomicInteger SKU = new AtomicInteger(10);

    @Override
    protected Application configure() {
        return Main.config();                                          // same application as production
    }

    // ------------------------------------------------------------------ helpers

    private Invocation.Builder read(String url) {
        return client().target(url).request().accept(MediaType.APPLICATION_JSON);
    }

    private Invocation.Builder write(String url) {
        return read(url).header("X-Api-Key", KEY);
    }

    private String productsUrl() {
        return target("products").getUri().toString();
    }

    private String ordersUrl() {
        return target("orders").getUri().toString();
    }

    private static String productJson(String sku, int stock) {
        return "{\"name\":\"Test " + sku + "\",\"sku\":\"" + sku + "\",\"price\":10.00,\"stock\":" + stock + "}";
    }

    /** Creates a product and returns its Location. */
    private String createProduct(int stock) {
        String sku = String.format("ZZ-%02d", SKU.getAndIncrement());
        try (Response r = write(productsUrl()).post(Entity.json(productJson(sku, stock)))) {
            assertEquals(201, r.getStatus());
            return r.getLocation().toString();
        }
    }

    private static long idOf(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private int stockOf(String productLocation) {
        Map<String, Object> product = read(productLocation).get(Map.class);
        return ((Number) product.get("stock")).intValue();
    }

    private static String orderBody(long productId, int quantity) {
        return "{\"lines\":[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]}";
    }

    private Response placeOrder(String idempotencyKey, String body) {
        Invocation.Builder request = write(ordersUrl());
        if (idempotencyKey != null) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }
        return request.post(Entity.json(body));
    }

    private static void assertProblem(Response r, int status) {
        assertEquals(status, r.getStatus());
        assertEquals(PROBLEM, r.getMediaType().toString());
    }

    // ------------------------------------------------------------------ products

    @Test
    void list_returns200_withTotalCountAndLinks() {
        try (Response r = target("products").queryParam("size", 5).request().get()) {
            assertEquals(200, r.getStatus());
            assertTrue(r.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE));
            assertNotNull(r.getHeaderString("X-Total-Count"));
            assertTrue(r.getHeaderString("Link").contains("rel=\"first\""));
            assertTrue(r.getHeaderString("Link").contains("rel=\"last\""));
        }
    }

    @Test
    void create_returns201_withLocation() {
        String sku = String.format("ZZ-%02d", SKU.getAndIncrement());
        try (Response r = write(productsUrl()).post(Entity.json(productJson(sku, 3)))) {
            assertEquals(201, r.getStatus());
            assertTrue(r.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE));
            assertNotNull(r.getLocation());
        }
    }

    @Test
    void delete_returns204_thenTheProductIsGone404() {
        String location = createProduct(1);
        try (Response r = write(location).delete()) {
            assertEquals(204, r.getStatus());
        }
        try (Response r = read(location).get()) {
            assertProblem(r, 404);
        }
    }

    @Test
    void unknownProduct_returns404_asProblemJson() {
        try (Response r = read(productsUrl() + "/999999").get()) {
            assertProblem(r, 404);
        }
    }

    @Test
    void conditionalGet_returns304_whenTheETagMatches() {
        String location = createProduct(1);
        String etag;
        try (Response r = read(location).get()) {
            assertEquals(200, r.getStatus());
            etag = r.getHeaderString("ETag");
            assertNotNull(etag);
            assertTrue(r.getHeaderString("Cache-Control").contains("max-age=60"));
        }
        try (Response r = read(location).header("If-None-Match", etag).get()) {
            assertEquals(304, r.getStatus());
        }
    }

    @Test
    void invalidBody_returns400_withOneErrorPerViolation() {
        try (Response r = write(productsUrl()).post(Entity.json("{\"name\":\"\",\"sku\":\"kettle\",\"price\":0,\"stock\":-1}"))) {
            assertProblem(r, 400);
            String body = r.readEntity(String.class);
            assertTrue(body.contains("\"errors\""));
            assertTrue(body.contains("\"field\":\"sku\""));
            assertTrue(body.contains("\"field\":\"stock\""));
        }
    }

    @Test
    void write_withoutApiKey_returns401() {
        try (Response r = client().target(productsUrl()).request().post(Entity.json(productJson("ZZ-90", 1)))) {
            assertProblem(r, 401);
            assertTrue(r.getHeaderString("WWW-Authenticate").startsWith("ApiKey"));
        }
    }

    @Test
    void notAcceptable_returns406() {
        try (Response r = client().target(productsUrl()).request().accept("text/csv").get()) {
            assertProblem(r, 406);
        }
    }

    @Test
    void duplicateSku_returns409() {
        String sku = String.format("ZZ-%02d", SKU.getAndIncrement());
        try (Response first = write(productsUrl()).post(Entity.json(productJson(sku, 1)))) {
            assertEquals(201, first.getStatus());
        }
        try (Response second = write(productsUrl()).post(Entity.json(productJson(sku, 1)))) {
            assertProblem(second, 409);
        }
    }

    @Test
    void put_withStaleETag_returns412() {
        String location = createProduct(1);
        try (Response r = write(location).header("If-Match", "\"99\"").put(Entity.json(productJson("ZZ-98", 5)))) {
            assertProblem(r, 412);
        }
    }

    @Test
    void put_withTheRightETag_returns200_andANewETag() {
        String location = createProduct(1);
        String etag;
        try (Response r = read(location).get()) {
            etag = r.getHeaderString("ETag");
        }
        try (Response r = write(location).header("If-Match", etag).put(Entity.json(productJson("ZZ-97", 5)))) {
            assertEquals(200, r.getStatus());
            assertNotNull(r.getHeaderString("ETag"));
            assertTrue(!etag.equals(r.getHeaderString("ETag")));
        }
    }

    @Test
    void wrongMediaType_returns415() {
        try (Response r = write(productsUrl()).post(Entity.text("Teapot"))) {
            assertProblem(r, 415);
        }
    }

    @Test
    void put_withoutIfMatch_returns428() {
        String location = createProduct(1);
        try (Response r = write(location).put(Entity.json(productJson("ZZ-96", 5)))) {
            assertProblem(r, 428);
        }
    }

    @Test
    void reviews_areValidated_andUnknownProductIs404() {
        String product = createProduct(1);
        try (Response r = write(product + "/reviews").post(Entity.json("{\"stars\":5,\"author\":\"Ana\",\"text\":\"Boils fast.\"}"))) {
            assertEquals(201, r.getStatus());
            assertNotNull(r.getLocation());
        }
        try (Response r = write(product + "/reviews").post(Entity.json("{\"stars\":9,\"author\":\"Ana\",\"text\":\"x\"}"))) {
            assertProblem(r, 400);
        }
        try (Response r = read(productsUrl() + "/999999/reviews").get()) {
            assertProblem(r, 404);
        }
    }

    // ------------------------------------------------------------------ orders

    @Test
    void placingTheSameOrderTwiceCreatesOneOrder() {
        String product = createProduct(10);
        String key = UUID.randomUUID().toString();
        String body = orderBody(idOf(product), 2);
        try (Response first = placeOrder(key, body); Response second = placeOrder(key, body)) {
            assertEquals(201, first.getStatus());
            assertEquals(201, second.getStatus());
            assertEquals("false", first.getHeaderString("Idempotency-Replayed"));
            assertEquals("true", second.getHeaderString("Idempotency-Replayed"));
            assertEquals(first.getLocation(), second.getLocation());
        }
        assertEquals(8, stockOf(product), "the stock is reserved only once");
    }

    @Test
    void sameKeyWithADifferentBody_returns422() {
        String product = createProduct(10);
        String key = UUID.randomUUID().toString();
        try (Response first = placeOrder(key, orderBody(idOf(product), 1))) {
            assertEquals(201, first.getStatus());
        }
        try (Response second = placeOrder(key, orderBody(idOf(product), 2))) {
            assertProblem(second, 422);
        }
    }

    @Test
    void placingAnOrderWithoutAnIdempotencyKey_returns428() {
        String product = createProduct(10);
        try (Response r = placeOrder(null, orderBody(idOf(product), 1))) {
            assertProblem(r, 428);
        }
    }

    @Test
    void concurrentRetriesCreateExactlyOneOrder() throws Exception {
        String product = createProduct(20);
        String key = UUID.randomUUID().toString();
        String body = orderBody(idOf(product), 1);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                results.add(pool.submit(() -> {
                    try (Response r = placeOrder(key, body)) {
                        assertEquals(201, r.getStatus());
                        return r.getLocation().toString();
                    }
                }));
            }
            Set<String> locations = new HashSet<>();
            for (Future<String> f : results) {
                locations.add(f.get(20, TimeUnit.SECONDS));
            }
            assertEquals(1, locations.size(), "all retries got the same order");
        } finally {
            pool.shutdown();
        }
        assertEquals(19, stockOf(product), "the stock is reserved only once");
    }

    @Test
    void order_unknownProduct_is404_andTooMuchIsAConflict409() {
        try (Response r = placeOrder(UUID.randomUUID().toString(), orderBody(999999, 1))) {
            assertProblem(r, 404);
        }
        String product = createProduct(2);
        try (Response r = placeOrder(UUID.randomUUID().toString(), orderBody(idOf(product), 3))) {
            assertProblem(r, 409);
        }
        assertEquals(2, stockOf(product), "a failed order reserves nothing");
    }

    @Test
    void order_invalidQuantity_returns400() {
        String product = createProduct(2);
        try (Response r = placeOrder(UUID.randomUUID().toString(), orderBody(idOf(product), 0))) {
            assertProblem(r, 400);
        }
    }

    @Test
    void order_canBePaid_followingOnlyTheLinks() {
        String product = createProduct(10);
        long productId = idOf(product);
        Map<String, Object> order;
        try (Response r = placeOrder(UUID.randomUUID().toString(), orderBody(productId, 2))) {
            assertEquals(201, r.getStatus());
            assertTrue(r.getHeaderString("Link").contains("rel=\"pay\""));
            order = r.readEntity(Map.class);
        }
        assertEquals("NEW", order.get("status"));
        Map<String, String> links = (Map<String, String>) order.get("_links");
        assertTrue(links.containsKey("self") && links.containsKey("pay") && links.containsKey("cancel"));
        assertEquals(product, links.get("product-" + productId));

        Map<String, Object> paid;
        try (Response r = write(links.get("pay")).put(Entity.text(""))) {
            assertEquals(200, r.getStatus());
            paid = r.readEntity(Map.class);
        }
        assertEquals("PAID", paid.get("status"));
        Map<String, String> paidLinks = (Map<String, String>) paid.get("_links");
        assertTrue(paidLinks.containsKey("self"));
        assertTrue(!paidLinks.containsKey("pay") && !paidLinks.containsKey("cancel"));

        try (Response r = write(links.get("pay")).put(Entity.text(""))) {
            assertProblem(r, 409);                                     // paying twice
        }
        try (Response r = write(links.get("cancel")).delete()) {
            assertProblem(r, 409);                                     // a PAID order cannot be cancelled
        }
    }

    @Test
    void order_canBeCancelled_andTheStockComesBack() {
        String product = createProduct(10);
        Map<String, Object> order;
        try (Response r = placeOrder(UUID.randomUUID().toString(), orderBody(idOf(product), 3))) {
            order = r.readEntity(Map.class);
        }
        assertEquals(7, stockOf(product));
        Map<String, String> links = (Map<String, String>) order.get("_links");
        try (Response r = write(links.get("cancel")).delete()) {
            assertEquals(200, r.getStatus());
            assertEquals("CANCELLED", r.readEntity(Map.class).get("status"));
        }
        assertEquals(10, stockOf(product));
    }

    // ------------------------------------------------------------------ OpenAPI

    @Test
    void openApiDocument_isServed() {
        assumeTrue(getClass().getResource("/META-INF/openapi.yaml") != null,
                "openapi.yaml is generated by the build: run mvn process-classes first");
        try (Response r = client().target(target("openapi").getUri()).request().get()) {
            assertEquals(200, r.getStatus());
            assertEquals("application/yaml", r.getMediaType().toString());
            String yaml = r.readEntity(String.class);
            assertTrue(yaml.contains("openapi:"));
            assertTrue(yaml.contains("/orders"));
            assertTrue(yaml.contains("/products"));
        }
    }
}
