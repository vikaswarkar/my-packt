package se.magnus.api.core.recommendation;

import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface RecommendationService {

    @GetMapping(value = "/recommendations", produces = "application/json")
    List<Recommendation> getRecommendations(@RequestParam(value = "productId", required = true) int productId);
    
    @PostMapping(value = "/recommendations", produces = "application/json", consumes = "application/json")
    public Recommendation createRecommendation(@RequestBody Recommendation recommendation);

    @DeleteMapping(value = "/recommendations")
    public void deleteRecommendations(@RequestParam("productId") int productId );

}
