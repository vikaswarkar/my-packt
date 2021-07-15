package se.magnus.microservices.composite.product.services.reactive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.composite.product.ReactiveProductCompositeService;
import se.magnus.api.composite.product.RecommendationSummary;
import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.util.http.ServiceUtil;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class ReactiveProductCompositeServiceImpl implements ReactiveProductCompositeService {

    private final ReactiveProductCompositeIntegration integration;
    private final ServiceUtil serviceUtil;

    public ReactiveProductCompositeServiceImpl(ReactiveProductCompositeIntegration integration, ServiceUtil serviceUtil) {
        this.integration = integration;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public void createCompositeProduct(ProductAggregate body) {
        try{
            log.info(String.format("Creating composite product for ProductId", body.getProductId()));
            Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            integration.createProduct(product);

            if (body.getRecommendations() != null){
                body.getRecommendations().stream().forEach(r -> {
                    Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
                    integration.createRecommendation(recommendation);
                });
            }
            if (body.getReviews() != null){
                body.getReviews().forEach(r -> {
                    Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
                    integration.createReview(review);
                });
            }
            log.info("Created composite product for {}", body.getProductId());
        }catch (RuntimeException ex){
            log.error("Create Composite Product failed {}", body.getProductId());
            throw ex;
        }
    }

    @Override
    public Mono<ProductAggregate> getCompositeProduct(int productId) {
        return Mono.zip(values -> createProductAggregate(
                (Product)values[0], (List<Recommendation>)values[1], (List<Review>) values[2]),
                integration.getProduct(productId),
                integration.getRecommendations(productId).collectList(),
                integration.getReviews(productId).collectList())
                .doOnError(ex->log.error(String.format("get CompositeProduct failed", ex.toString())))
                .log();
    }

    @Override
    public void deleteCompositeProduct(int productId) {
        try{
            log.info("Deleting composite product for {}", productId);
            integration.deleteProduct(productId);
            integration.deleteRecommendations(productId);
            integration.deleteReviews(productId);
            log.info("Successfully deleted composite product for {}", productId);
        }catch(RuntimeException ex){
            log.error("Error deleting composite product for {}", productId);
            throw ex;
        }
    }

    private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews) {
        return ProductAggregate.builder().productId(product.getProductId())
                .name(product.getName())
                .weight(product.getWeight())
                .recommendations((recommendations == null ? null :
                        recommendations.stream().map(
                                r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent())
                        ).collect(Collectors.toList())
                ))
                .reviews(reviews == null ? null :
                        reviews.stream().map(
                                r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent())
                        ).collect(Collectors.toList())).build();
    }
}
