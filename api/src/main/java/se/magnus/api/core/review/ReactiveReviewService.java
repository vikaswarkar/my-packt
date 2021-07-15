package se.magnus.api.core.review;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/reactive")
public interface ReactiveReviewService {

    @GetMapping(value="/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
    Flux<Review> getReviews(@RequestParam("productId")int productId);

    @DeleteMapping("/reviews")
    void deleteReviews(@RequestParam("productId")int productId);

    @PostMapping
    public Mono<Review> createReview(@RequestBody Review body);
}
