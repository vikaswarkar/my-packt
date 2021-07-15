package se.magnus.api.core.review;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/review")
public interface ReviewService {

    /**
     * Sample usage: curl $HOST:$PORT/review?productId=1
     *
     * @param productId
     * @return
     */
    @GetMapping(
        produces = "application/json")
    List<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);
    
    @PostMapping(produces = "application/json", consumes = "application/json")
    public Review createReview(@RequestBody Review review);
    
    @DeleteMapping
    public void deleteReviews(@RequestParam("productId")int productId);
    
}