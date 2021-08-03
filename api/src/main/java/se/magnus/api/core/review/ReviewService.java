package se.magnus.api.core.review;

import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface ReviewService {

    @GetMapping(value = "/reviews", produces = "application/json")
    List<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);
    
    @PostMapping(value = "/reviews", produces = "application/json", consumes = "application/json")
    public Review createReview(@RequestBody Review review);
    
    @DeleteMapping(value = "/reviews")
    public void deleteReviews(@RequestParam("productId")int productId);
    
}