package se.magnus.microservices.core.recommendation.services;

import com.mongodb.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.magnus.api.core.recommendation.ReactiveRecommendationService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistence.ReactiveRecommendationRepository;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

@RestController
@Slf4j
public class ReactiveRecommendationServiceImpl implements ReactiveRecommendationService {

    private final ReactiveRecommendationRepository repository;
    private final RecommendationMapper mapper;
    private final ServiceUtil serviceUtil;


    public ReactiveRecommendationServiceImpl(ReactiveRecommendationRepository repository,
                                             RecommendationMapper mapper, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid ProductId: " + productId);
        return repository.findByProductId(productId)
                .log()
                .map(e-> mapper.entityToApi(e))
                .map(e-> {e.setServiceAddress(serviceUtil.getServiceAddress());
                        return e;});
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation recommendation) {
        if (recommendation.getProductId() < 1) throw new InvalidInputException("Invalid ProductId: " + recommendation.getProductId());
        RecommendationEntity entityToSave = mapper.apiToEntity(recommendation);
        return  repository.save(entityToSave)
                .log()
                .onErrorMap(DuplicateKeyException.class,
                        ex-> new InvalidInputException(
                                String.format("Duplicate Key, Product Id: %s, Recommendation Id %s",
                                        recommendation.getProductId(), recommendation.getRecommendationId() )))
                .map(e -> mapper.entityToApi(e));
    }

    @Override
    public void deleteRecommendations(int productId) {
        if (productId < 1 ) throw new InvalidInputException("Invalid ProductId: " + productId);
        log.info(String.format("Delete recommendations for Product Id %s", productId));
        repository.deleteAll(repository.findByProductId(productId)).block();
    }
}
