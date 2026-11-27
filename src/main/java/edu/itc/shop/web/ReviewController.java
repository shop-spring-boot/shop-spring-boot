package edu.itc.shop.web;

import edu.itc.shop.domain.Review;
import edu.itc.shop.repository.ReviewRepository;
import edu.itc.shop.web.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {
    private final ReviewRepository reviews;
    public ReviewController(ReviewRepository reviews) { this.reviews = reviews; }

    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewResponse> create(@RequestBody @Valid ReviewRequest req, Authentication auth) {
        Review r = reviews.save(new Review(req.productId(), auth.getName(), req.stars(), req.text()));
        return ResponseEntity.status(201).body(ReviewResponse.from(r));
    }

    @GetMapping("/api/products/{id}/reviews")
    public List<ReviewResponse> list(@PathVariable Long id) {
        return reviews.findByProductId(id).stream().map(ReviewResponse::from).toList();
    }
}