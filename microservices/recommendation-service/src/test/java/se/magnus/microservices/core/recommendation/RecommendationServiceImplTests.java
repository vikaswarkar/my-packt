package se.magnus.microservices.core.recommendation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static reactor.core.publisher.Mono.just;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {"spring.data.mongodb.port:0", "eureka.client.enabled=false"})
public class RecommendationServiceImplTests {

	private String RECOMMENDATION_BASE_URL = "/recommendations";

	@Autowired
	private WebTestClient client;

	@Autowired
	RecommendationRepository repository;
	
	@Test
	public void setupDb() {
		repository.deleteAll();
	}
	
	@Test
	@Disabled
		public void getRecommendationsByProductId() {

		int productId = 1;

		postAndVerifyRecommendation(productId, 1, HttpStatus.OK);
		postAndVerifyRecommendation(productId, 2, HttpStatus.OK);
		postAndVerifyRecommendation(productId, 3, HttpStatus.OK);
		
		client.get()
			.uri(RECOMMENDATION_BASE_URL + "?productId=" + productId)
			.accept(APPLICATION_JSON_UTF8)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON_UTF8)
			.expectBody()
			.jsonPath("$.length()").isEqualTo(3)
			.jsonPath("$[0].productId").isEqualTo(productId);
	}

	@Test
	public void getRecommendationsMissingParameter() {

		client.get()
			.uri(RECOMMENDATION_BASE_URL)
			.accept(APPLICATION_JSON_UTF8)
			.exchange()
			.expectStatus().isEqualTo(BAD_REQUEST)
			.expectHeader().contentType(APPLICATION_JSON_UTF8)
			.expectBody()
			.jsonPath("$.path").isEqualTo(RECOMMENDATION_BASE_URL);
//			.jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
	}

	@Test
	public void getRecommendationsInvalidParameter() {

		client.get()
			.uri(RECOMMENDATION_BASE_URL + "?productId=no-integer")
			.accept(APPLICATION_JSON_UTF8)
			.exchange()
			.expectStatus().isEqualTo(BAD_REQUEST)
			.expectHeader().contentType(APPLICATION_JSON_UTF8)
			.expectBody()
			.jsonPath("$.path").isEqualTo(RECOMMENDATION_BASE_URL);
//			.jsonPath("$.message").isEqualTo("Type mismatch.");
	}

	@Test
	public void getRecommendationsNotFound() {

		int productIdNotFound = 113;

		client.get()
			.uri(RECOMMENDATION_BASE_URL + "?productId=" + productIdNotFound)
			.accept(APPLICATION_JSON_UTF8)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(APPLICATION_JSON_UTF8)
			.expectBody()
			.jsonPath("$.length()").isEqualTo(0);
	}

	@Test
	public void getRecommendationsInvalidParameterNegativeValue() {

		int productIdInvalid = -1;

		client.get()
			.uri(RECOMMENDATION_BASE_URL + "?productId=" + productIdInvalid)
			.accept(APPLICATION_JSON_UTF8)
			.exchange()
			.expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
			.expectHeader().contentType(APPLICATION_JSON_UTF8)
			.expectBody()
			.jsonPath("$.path").isEqualTo(RECOMMENDATION_BASE_URL)
			.jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
	}
	
	private WebTestClient.BodyContentSpec postAndVerifyRecommendation(int productId, int recommendationId, HttpStatus expectedStatus){
		Recommendation recommendation = new Recommendation(productId, recommendationId, "Author", 4, "Contect", "SA");

		return client.post()
		.uri(RECOMMENDATION_BASE_URL + "?productId="+ productId)
		.body(just(recommendation), Recommendation.class)
		.accept(MediaType.APPLICATION_JSON_UTF8)
		.exchange()
		.expectStatus().isEqualTo(expectedStatus)
		.expectHeader().contentType(APPLICATION_JSON_UTF8)
		.expectBody();
		
	}
}
