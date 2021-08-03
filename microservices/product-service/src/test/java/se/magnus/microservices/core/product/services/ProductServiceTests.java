package se.magnus.microservices.core.product.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.product.Product;
import se.magnus.microservices.core.product.persistence.ProductRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static reactor.core.publisher.Mono.just;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {"spring.data.mongodb.port:0", "eureka.client.enabled=false"})
public class ProductServiceTests {

	private String PRODUCT_BASE_URL = "/products";

	@Autowired
    private WebTestClient client;

    @Autowired
    private ProductRepository repository;
    
    @Test
    public void contextLoads() {
    }
    
    @BeforeEach
    public void setupDb() {
    	repository.deleteAll();
    }
    
	@Test
	public void getProductById() {
		int productId = 1;

		postAndVerifyProduct(productId, HttpStatus.OK);

		assertTrue(repository.findByProductId(productId).isPresent());

		getAndVerifyProduct(productId, HttpStatus.OK)
			.jsonPath("$.productId").isEqualTo(productId)
			.jsonPath("$.name").isEqualTo("Name " + productId);
	}

	@Test
	public void duplicateError() {
		int productId = 1;
		postAndVerifyProduct(productId, HttpStatus.OK);
		assertTrue(repository.findByProductId(productId).isPresent());
		postAndVerifyProduct(productId, UNPROCESSABLE_ENTITY)
		.jsonPath("$.path").isEqualTo(PRODUCT_BASE_URL )
		.jsonPath("$.message").isEqualTo("Duplicate Key, Product Id " + productId);
	}
	
	@Test
	public void deleteProduct() {
		int productId = 1;
		postAndVerifyProduct(1, HttpStatus.OK);
		assertTrue(repository.findByProductId(productId).isPresent());
		deleteAndVerifyProduct(productId, HttpStatus.OK);
		assertFalse(repository.findByProductId(productId).isPresent());
		deleteAndVerifyProduct(productId, HttpStatus.OK);
	}
	
	@Test
	public void getProductInvalidParameterString() {
        client.get()
            .uri(PRODUCT_BASE_URL + "/no-integer")
            .accept(APPLICATION_JSON_UTF8)
            .exchange()
            .expectStatus().isEqualTo(BAD_REQUEST)
            .expectHeader().contentType(APPLICATION_JSON_UTF8)
            .expectBody()
            .jsonPath("$.path").isEqualTo(PRODUCT_BASE_URL + "/no-integer");
//            .jsonPath("$.message").isEqualTo("Type mismatch.");
	}

	@Test
	public void getProductNotFound() {
		int productIdNotFound = 13;
        client.get()
            .uri(PRODUCT_BASE_URL + "/" + productIdNotFound)
            .accept(APPLICATION_JSON_UTF8)
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentType(APPLICATION_JSON_UTF8)
            .expectBody()
            .jsonPath("$.path").isEqualTo(PRODUCT_BASE_URL + "/" + productIdNotFound)
            .jsonPath("$.message").isEqualTo("No Product found for productId " + productIdNotFound);
	}

	@Test
	public void getProductInvalidParameterNegativeValue() {
        int productIdInvalid = -1;
        client.get()
            .uri(PRODUCT_BASE_URL + "/" + productIdInvalid)
            .accept(APPLICATION_JSON_UTF8)
            .exchange()
            .expectStatus().isEqualTo(UNPROCESSABLE_ENTITY)
            .expectHeader().contentType(APPLICATION_JSON_UTF8)
            .expectBody()
            .jsonPath("$.path").isEqualTo(PRODUCT_BASE_URL + "/" + productIdInvalid)
            .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
	}

	private WebTestClient.BodyContentSpec postAndVerifyProduct(int productId, HttpStatus expectedStatus){
		Product product = new Product(productId, "Name " + productId, productId, "SA");
		return client.post()
				.uri(PRODUCT_BASE_URL)
				.body(just(product), Product.class)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON_UTF8)
				.expectBody();
	}
	
	private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus){
		return getAndVerifyProduct("/" + productId, expectedStatus);
	}
	
	private WebTestClient.BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus){
		return client.get()
				.uri(PRODUCT_BASE_URL  + productIdPath)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON_UTF8)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec deleteAndVerifyProduct(int productId, HttpStatus expectedStatus){
		return client.delete()
				.uri(PRODUCT_BASE_URL + "/" + productId)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}
}

