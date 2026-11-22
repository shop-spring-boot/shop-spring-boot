package edu.itc.shop;

import java.net.URI;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import edu.itc.shop.api.IdempotencyStore;
import edu.itc.shop.domain.InMemoryProductRepository;
import edu.itc.shop.domain.OrderService;
import edu.itc.shop.domain.ProductRepository;
import edu.itc.shop.domain.ReviewRepository;

public class Main {
    public static final URI BASE = URI.create("http://localhost:8080/api/");

    /** The application: scanned resources/providers + one shared instance of each repository and service. */
    public static ResourceConfig config() {
        ProductRepository products = new InMemoryProductRepository();
        OrderService orders = new OrderService(products);          // reserves stock in the same repository
        ReviewRepository reviews = new ReviewRepository();
        IdempotencyStore idempotency = new IdempotencyStore();
        return new ResourceConfig()
                .packages("edu.itc.shop.api")
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(products).to(ProductRepository.class);
                        bind(orders).to(OrderService.class);
                        bind(reviews).to(ReviewRepository.class);
                        bind(idempotency).to(IdempotencyStore.class);
                    }
                });
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE, config());
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        System.out.println("shop-rest listening on " + BASE);
        Thread.currentThread().join();
    }
}
