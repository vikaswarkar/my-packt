package se.magnus.api.core.recommendation;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RequestMapping("/async")
public interface AsyncRecommendationService {

    @GetMapping(value = "/recommendations", produces = "application/json")
    public Flux<Recommendation> getRecommendations(@RequestParam(value = "productId", required = true) int productId);

    @PostMapping("/recommendations")
    public Recommendation createRecommendation(@RequestBody Recommendation recommendation);

    @DeleteMapping("/recommendations")
    public void deleteRecommendations(@RequestParam(value = "productId", required = true)int productId);
}
