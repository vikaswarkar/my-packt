package se.magnus.microservices.composite.product.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;

@Component
@Slf4j
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

	private final RestTemplate restTemplate;
	private final ExceptionHelper exceptionHelper;

	private final String productServiceUrl;
	private final String recommendationServiceUrl;
	private final String reviewServiceUrl;

	@Autowired
	public ProductCompositeIntegration(
			RestTemplate restTemplate,
			ExceptionHelper exceptionHelper,

			@Value("${app.product-service.host}") String productServiceHost,
			@Value("${app.product-service.port}") int productServicePort,

			@Value("${app.recommendation-service.host}") String recommendationServiceHost,
			@Value("${app.recommendation-service.port}") int recommendationServicePort,

			@Value("${app.review-service.host}") String reviewServiceHost,
			@Value("${app.review-service.port}") int reviewServicePort
	) {

		this.restTemplate = restTemplate;
		this.exceptionHelper = exceptionHelper;

		productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product/";
		recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation?productId=";
		reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";
	}

	@Override
	public Product createProduct(Product body) {
		try {
			log.debug("Will post a new product to the new URL {} ", this.productServiceUrl);
			Product product = restTemplate.postForObject(this.productServiceUrl, body, Product.class);

			log.debug("Created product {}.", product);
			return product;

		} catch (HttpClientErrorException ex) {
			throw handleHttpClientException(ex);
		}
	}

	@Override
	public Product getProduct(int productId) {

		try {
			String url = productServiceUrl + productId;
			log.debug("Will call getProduct API on URL: {}", url);

			Product product = restTemplate.getForObject(url, Product.class);
			log.debug("Found a product with id: {}", product.getProductId());

			return product;

		} catch (HttpClientErrorException ex) {
			throw handleHttpClientException(ex);
		}
	}

	@Override
	public void deleteProduct(int productId) {
		try {
			String url = this.productServiceUrl + productId;
			log.debug("Will call delete api on {} ", url);
			this.restTemplate.delete(url);
		} catch (HttpClientErrorException ex) {
			throw this.handleHttpClientException(ex);
		}
	}

	@Override
	public Recommendation createRecommendation(Recommendation body) {
		try {
			String url = this.recommendationServiceUrl;
			Recommendation recommendation = restTemplate.postForObject(url, body, Recommendation.class);
			return recommendation;
		} catch (HttpClientErrorException ex) {
			throw this.handleHttpClientException(ex);
		}
	}

	public List<Recommendation> getRecommendations(int productId) {

		try {
			String url = recommendationServiceUrl + productId;

			log.debug("Will call getRecommendations API on URL: {}", url);
			List<Recommendation> recommendations = restTemplate.exchange(url, GET, null, new ParameterizedTypeReference<List<Recommendation>>() {
			}).getBody();

			log.debug("Found {} recommendations for a product with id: {}", recommendations.size(), productId);
			return recommendations;

		} catch (Exception ex) {
			log.warn("Got an exception while requesting recommendations, return zero recommendations: {}", ex.getMessage());
			return new ArrayList<>();
		}
	}

	@Override
	public void deleteRecommendations(int productId) {
		try {
			String url = this.recommendationServiceUrl + productId;
			log.debug("Will call the delete recommendations api on {}", url);
			restTemplate.delete(url);
		} catch (HttpClientErrorException ex) {
			this.handleHttpClientException(ex);
		}
	}

	@Override
	public Review createReview(Review body) {
		try {
			String url = this.reviewServiceUrl;
			log.debug("Will call create review on {}", url);
			Review review = this.restTemplate.postForObject(url, body, Review.class);
			return review;
		} catch (HttpClientErrorException ex) {
			throw this.handleHttpClientException(ex);
		}
	}

	public List<Review> getReviews(int productId) {

		try {
			String url = reviewServiceUrl + productId;

			log.debug("Will call getReviews API on URL: {}", url);
			List<Review> reviews = restTemplate.exchange(url, GET, null, new ParameterizedTypeReference<List<Review>>() {
			}).getBody();

			log.debug("Found {} reviews for a product with id: {}", reviews.size(), productId);
			return reviews;

		} catch (Exception ex) {
			log.warn("Got an exception while requesting reviews, return zero reviews: {}", ex.getMessage());
			return new ArrayList<>();
		}
	}

	@Override
	public void deleteReviews(int productId) {
		try {
			String url = this.reviewServiceUrl + productId;
			log.debug("Will call deleteReviews  on ", url);
		} catch (HttpClientErrorException ex) {
			this.handleHttpClientException(ex);
		}
	}

	private RuntimeException handleHttpClientException(HttpClientErrorException ex) {
		return exceptionHelper.handleException(ex, ex.getStatusCode());
	}

}