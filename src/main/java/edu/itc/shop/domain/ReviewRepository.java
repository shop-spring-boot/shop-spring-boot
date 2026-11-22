package edu.itc.shop.domain;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory reviews, shared by all requests (one bound singleton, so it must be thread-safe). */
public class ReviewRepository {

    private final List<Review> reviews = new CopyOnWriteArrayList<>();
    private final AtomicLong ids = new AtomicLong();

    public List<Review> findByProduct(long productId) {
        return reviews.stream().filter(r -> r.productId() == productId).toList();
    }

    public Review add(long productId, int stars, String author, String text) {
        Review review = new Review(ids.incrementAndGet(), productId, stars, author, text);
        reviews.add(review);
        return review;
    }
}
