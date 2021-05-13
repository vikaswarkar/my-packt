package se.magnus.microservices.core.review.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.microservices.core.review.persistence.ReviewEntity;
import se.magnus.microservices.core.review.persistence.ReviewRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

@Slf4j
@RestController
public class ReviewServiceImpl implements ReviewService {


    private final ServiceUtil serviceUtil;
    private final ReviewRepository repository;
    private final ReviewMapper mapper;
    
    @Autowired
    public ReviewServiceImpl(ServiceUtil serviceUtil, ReviewRepository repository, ReviewMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Review> getReviews(int productId) {

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        List<ReviewEntity> entityList = repository.findByProductId(productId);
        List<Review> reviews = mapper.entityListToApiList(entityList);
        reviews.forEach(r-> r.setServiceAddress(serviceUtil.getServiceAddress()));	
        log.debug("getReviews response size: {}", reviews.size());

        return reviews;
    }

	@Override
	public Review createReview(Review review) {
		try {
			ReviewEntity entity = mapper.apiToEntity(review);
			entity = repository.save(entity);
			log.debug("Review Created" + review.toString());
			review = mapper.entityToApi(entity);
			return review;
		}catch(DataIntegrityViolationException ex) {
			throw new InvalidInputException(String.format("Duplicate Key, Product Id: %s, Review Id: %s" , review.getProductId(), review.getReviewId()));
		}
	}

	@Override
	public void deleteReviews(int productId) {
		repository.deleteAll(repository.findByProductId(productId));
	}
}
