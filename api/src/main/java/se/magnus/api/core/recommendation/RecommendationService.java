package se.magnus.api.core.recommendation;

import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface RecommendationService {

    /**
     * Sample usage: curl $HOST:$PORT/recommendation?productId=1
     *
     * @param productId
     * @return
     */
    @GetMapping(
        value    = "/recommendation",
        produces = "application/json")
    List<Recommendation> getRecommendations(@RequestParam(value = "productId", required = true) int productId);
    
    @PostMapping(value = "/recommendation", produces = "application/json", consumes = "application/json")
    public Recommendation createRecommendation(@RequestBody Recommendation recommendation);

    @DeleteMapping(value = "/recommendation")
    public void deleteRecommendations(@RequestParam("productId") int productId );

}
