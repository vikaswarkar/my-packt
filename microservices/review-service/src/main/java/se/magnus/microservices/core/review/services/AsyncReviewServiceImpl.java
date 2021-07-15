package se.magnus.microservices.core.review.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import se.magnus.api.core.review.AsyncReviewService;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.core.review.persistence.ReviewEntity;
import se.magnus.microservices.core.review.persistence.ReviewRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

import java.util.List;

@RestController
@Slf4j
public class AsyncReviewServiceImpl implements AsyncReviewService {

    private final ReviewRepository repository;

    private final ReviewMapper mapper;

    private final ServiceUtil serviceUtil;

    private final Scheduler scheduler;

    public AsyncReviewServiceImpl(ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil, Scheduler scheduler) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
        this.scheduler = scheduler;
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        if (productId < 1 ) throw new InvalidInputException("Invalid ProductId:" + productId);
        return asyncFlux(getByProductId(productId));
    }

    protected List<Review> getByProductId(int productId){
        List<ReviewEntity> reviewEntities = repository.findByProductId(productId);
        List<Review> reviews = mapper.entityListToApiList(reviewEntities);
        reviews.forEach(r -> r.setServiceAddress(serviceUtil.getServiceAddress()));
        log.info("getReviews: Response Size: ", reviews.size());
        return reviews;
    }

    @Override
    public void deleteReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
        log.info("Deleting all the reviews for " + productId);
        repository.deleteAll(repository.findByProductId(productId));
    }

    @Override
    public Review createReview(Review body) {
        if (body.getProductId() < 1) throw new InvalidInputException("Inbalid Product Id " + body.getProductId());
        try{
            ReviewEntity entity = mapper.apiToEntity(body);
            entity = repository.save(entity);
            log.info("Create Review entity {}/{} ", entity.getProductId(), entity.getReviewId());
            return mapper.entityToApi(entity);
        }catch (DataIntegrityViolationException dive){
            throw new InvalidInputException("Duplicate Key, Product Id: " + body.getProductId());
        }
    }

    private <T> Flux<T> asyncFlux(Iterable<T> iterable){
        return Flux.fromIterable(iterable).publishOn(scheduler);
    }
}
