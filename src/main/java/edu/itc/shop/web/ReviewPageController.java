package edu.itc.shop.web;

import edu.itc.shop.repository.ReviewRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ReviewPageController {
    private final ReviewRepository reviews;
    public ReviewPageController(ReviewRepository reviews) { this.reviews = reviews; }

    @GetMapping("/products/{id}/reviews.html")
    public String page(@PathVariable Long id, Model model) {
        model.addAttribute("productId", id);
        model.addAttribute("reviews", reviews.findByProductId(id));
        return "reviews";
    }
}