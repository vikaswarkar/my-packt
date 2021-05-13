package se.magnus.microservices.core.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static reactor.core.publisher.Mono.just;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.core.review.persistence.ReviewRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@Slf4j
public class ReviewServiceApplicationTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	ReviewRepository repository;
	
	@BeforeEach
	public void setUp() {
		repository.deleteAll();
	}
	
	@Test
	public void getReviewsByProductId() {

		assertEquals(0, repository.count());
		
		postAndVerifyReview(1, 1, HttpStatus.OK);
		postAndVerifyReview(1, 2, HttpStatus.OK);
		postAndVerifyReview(1, 3, HttpStatus.OK);
		
		assertEquals(3, repository.count());
		
		
		int productId = 1;
		printJson(productId);
		
		getAndVerifyReviewsByProductId(productId, HttpStatus.OK)
		.jsonPath("$.length()").isEqualTo(3)
		.jsonPath("$[0].productId").isEqualTo(productId);
	}

	private void printJson(int productId) {
		WebClient webClient = WebClient.create();
		
		log.info("JSON ==>"+ webClient.get().uri("http://localhost:7003/review?productId="+productId).exchange()
				.block().bodyToMono(String.class).block());
		
	}
	
	@Test
	public void duplicateError() {
		int productId = 1, reviewId = 1;
		
		postAndVerifyReview(productId, reviewId, HttpStatus.OK)
		.jsonPath("$.productId").isEqualTo(productId)
		.jsonPath("$.reviewId").isEqualTo(reviewId);
		
		assertEquals(1, repository.count());
		
		postAndVerifyReview(productId, reviewId, HttpStatus.UNPROCESSABLE_ENTITY)
		.jsonPath("$.path").isEqualTo("/review")
		.jsonPath("$.message").isEqualTo("Duplicate Key, Product Id: 1, Review Id: 1");
		
	}
	
	@Test
	public void deleteReviews() {
		int productId = 1;
		int reviewId = 1;
		postAndVerifyReview(productId, reviewId, HttpStatus.OK);
		assertEquals(1,repository.count());
		
		deleteAndVerifyReviewsByProductId(1, HttpStatus.OK);
		assertEquals(0,repository.count());
		
		deleteAndVerifyReviewsByProductId(1, HttpStatus.OK);
		assertEquals(0,repository.count());

	}
	
	@Test
	public void getReviewsMissingParameter() {

		client.get().uri("/review").accept(APPLICATION_JSON_UTF8).exchange().expectStatus().isEqualTo(BAD_REQUEST)
				.expectHeader().contentType(APPLICATION_JSON_UTF8).expectBody().jsonPath("$.path").isEqualTo("/review");
//				.jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
	}

	@Test
	public void getReviewsInvalidParameter() {

		client.get()
			.uri("/review?productId=no-integer")
			.accept(APPLICATION_JSON_UTF8)
			.exchange()
			.expectStatus().isEqualTo(BAD_REQUEST)
			.expectHeader().contentType(APPLICATION_JSON_UTF8)
			.expectBody()
			.jsonPath("$.path").isEqualTo("/review");
//			.jsonPath("$.message").isEqualTo("Type mismatch.");
	}

	@Test
	public void getReviewsNotFound() {

		int productIdNotFound = 213;

		client.get().uri("/review?productId=" + productIdNotFound).accept(APPLICATION_JSON_UTF8).exchange()
				.expectStatus().isOk().expectHeader().contentType(APPLICATION_JSON_UTF8).expectBody()
				.jsonPath("$.length()").isEqualTo(0);
	}

	@Test
	public void getReviewsInvalidParameterNegativeValue() {

		int productIdInvalid = -1;

		client.get().uri("/review?productId=" + productIdInvalid).accept(APPLICATION_JSON_UTF8).exchange()
				.expectStatus().isEqualTo(UNPROCESSABLE_ENTITY).expectHeader().contentType(APPLICATION_JSON_UTF8)
				.expectBody().jsonPath("$.path").isEqualTo("/review").jsonPath("$.message")
				.isEqualTo("Invalid productId: " + productIdInvalid);
	}
	
	private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus){
		return client.get()
				.uri("/review?productId="+ productId)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON_UTF8)
				.expectBody();
	}
	
	private WebTestClient.BodyContentSpec postAndVerifyReview(int productId, int reviewId, HttpStatus expectedStatus){
		Review review = new Review(productId, reviewId, "Author"+reviewId, "Subject" + reviewId, "Content" + reviewId,  "SA");
		return client.post()
				.uri("/review")
				.body(just(review), Review.class)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON_UTF8)
				.expectBody();
	}
	
	private WebTestClient.BodyContentSpec deleteAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus){
		return client
				.delete()
				.uri("/review?productId="+productId)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}
}
