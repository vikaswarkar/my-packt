package se.magnus.microservices.core.product.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ReactiveProductRepository extends ReactiveCrudRepository<ProductEntity, String> {

	public Mono<ProductEntity> findByProductId(int productId);
	
}
