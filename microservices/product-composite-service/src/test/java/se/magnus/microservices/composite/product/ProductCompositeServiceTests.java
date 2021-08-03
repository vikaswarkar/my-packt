package se.magnus.microservices.composite.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.composite.product.RecommendationSummary;
import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static reactor.core.publisher.Mono.just;

@SpringBootTest(webEnvironment=RANDOM_PORT,
		properties = {"eureka.client.enabled=false"})
@ExtendWith(SpringExtension.class)
public class ProductCompositeServiceTests {

	private static final int PRODUCT_ID_OK = 1;
	private static final int PRODUCT_ID_NOT_FOUND = 2;
	private static final int PRODUCT_ID_INVALID = 3;

    @Autowired
    private WebTestClient client;

	@MockBean
	private ProductCompositeIntegration compositeIntegration;

	@BeforeEach
	public void setUp() {

		when(compositeIntegration.getProduct(PRODUCT_ID_OK)).
			thenReturn(new Product(PRODUCT_ID_OK, "name", 1, "mock-address"));
		
		when(compositeIntegration.getRecommendations(PRODUCT_ID_OK)).
			thenReturn(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address")));
		
		when(compositeIntegration.getReviews(PRODUCT_ID_OK)).
			thenReturn(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address")));

		when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND)).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

		when(compositeIntegration.getProduct(PRODUCT_ID_INVALID)).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
	}

	@Test
	public void contextLoads() {
	}

	@Test
	public void createCompositeProduct1() {
		ProductAggregate compositeProduct = new ProductAggregate(PRODUCT_ID_OK, "name", 1, null, null, null);
		postAndVerifyProduct(compositeProduct, HttpStatus.OK);
	}
	
	@Test
	public void createCompositeProduct2() {
		ProductAggregate compositeProduct = new ProductAggregate(PRODUCT_ID_OK, "name", 1, 
				singletonList(new RecommendationSummary(1, "Author", 1, "Content")), 
				singletonList(new ReviewSummary(1, "Author", "Subject", "Content")), null);
		postAndVerifyProduct(compositeProduct, HttpStatus.OK);
	}
	
	@Test
	public void getProductById() {

        client.get()
            .uri("/product-composite/" + PRODUCT_ID_OK)
            .accept(APPLICATION_JSON_UTF8)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(APPLICATION_JSON_UTF8)
            .expectBody()
            .jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
            .jsonPath("$.recommendations.length()").isEqualTo(1)
            .jsonPath("$.reviews.length()").isEqualTo(1);
	}

	@Test
	public void getProductNotFound() {

        client.get()
            .uri("/product-composite/" + PRODUCT_ID_NOT_FOUND)
            .accept(APPLICATION_JSON_UTF8)
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentType(APPLICATION_JSON_UTF8)
            .expectBody()
            .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
            .jsonPath("$.message").isEqualTo("NOT FOUND: " + PRODUCT_ID_NOT_FOUND);
	}

	@Test
	public void getProductInvalidInput() {

        client.get()
            .uri("/product-composite/" + PRODUCT_ID_INVALID)
            .accept(APPLICATION_JSON_UTF8)
            .exchange()
            .expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
            .expectHeader().contentType(APPLICATION_JSON_UTF8)
            .expectBody()
            .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_INVALID)
            .jsonPath("$.message").isEqualTo("INVALID: " + PRODUCT_ID_INVALID);
	}
	
	private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus ){
		return client.get()
		.uri("/product-composite/"+ productId)
		.accept(APPLICATION_JSON_UTF8)
        .exchange()
        .expectStatus().isEqualTo(expectedStatus)
        .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody();
		
	}
	
	private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		client.delete()
				.uri("/product-composite")
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}
	
	
	private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus){
		client.post()
				.uri("/product-composite")
				.body(just(compositeProduct), ProductAggregate.class)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}
}
