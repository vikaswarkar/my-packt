package se.magnus.microservices.core.recommendation.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
@Repository
public interface ReactiveRecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, String> {

	public Flux<RecommendationEntity> findByProductId(int productId);
}
