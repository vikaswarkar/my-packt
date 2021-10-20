package se.magnus.microservices.composite.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
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
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(webEnvironment=RANDOM_PORT,
		classes = {ProductCompositeServiceApplication.class, TestSecurityConfig.class},
		properties = {"eureka.client.enabled=false",
				"spring.main.allow-bean-definition-overriding=true"})
@ExtendWith(SpringExtension.class)
public class ProductCompositeServiceTests {

	private static final int PRODUCT_ID_OK = 1;
	private static final int PRODUCT_ID_NOT_FOUND = 2;
	private static final int PRODUCT_ID_INVALID = 3;

	MockMvc mockMvc;

	@Autowired
	WebApplicationContext webApplicationContext;

	@MockBean
	private ProductCompositeIntegration compositeIntegration;

	@BeforeEach
	public void setUp() {

		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

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
	public void createCompositeProduct1() throws Exception {
		ProductAggregate compositeProduct = new ProductAggregate(PRODUCT_ID_OK, "name", 1, null, null, null);
		postAndVerifyProduct(compositeProduct, HttpStatus.OK);
	}
	
	@Test
	public void createCompositeProduct2() throws Exception {
		ProductAggregate compositeProduct = new ProductAggregate(PRODUCT_ID_OK, "name", 1, 
				singletonList(new RecommendationSummary(1, "Author", 1, "Content")), 
				singletonList(new ReviewSummary(1, "Author", "Subject", "Content")), null);
		postAndVerifyProduct(compositeProduct, HttpStatus.OK);
	}
	
	@Test
	public void getProductById() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/product-composite/" + PRODUCT_ID_OK)
				.accept(APPLICATION_JSON_VALUE)
				.contentType(APPLICATION_JSON_VALUE))
						.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
				.andExpect(MockMvcResultMatchers.jsonPath("$.productId").value(PRODUCT_ID_OK))
				.andExpect(MockMvcResultMatchers.jsonPath("$.recommendations.length()").value(1))
				.andExpect(MockMvcResultMatchers.jsonPath("$.reviews.length()").value(1));
	}

	@Test
	public void getProductNotFound() throws Exception {
		val notFound = HttpStatus.NOT_FOUND;
		mockMvc.perform(MockMvcRequestBuilders.get("/product-composite/" + PRODUCT_ID_NOT_FOUND)
				.accept(APPLICATION_JSON_VALUE)
				.contentType(APPLICATION_JSON_VALUE))
						.andExpect(MockMvcResultMatchers.status().is(notFound.value()))
								.andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
				.andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/product-composite/" + PRODUCT_ID_NOT_FOUND))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));
	}

	@Test
	public void getProductInvalidInput() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/product-composite/" + PRODUCT_ID_INVALID)
				.accept(APPLICATION_JSON_VALUE)
				.contentType(APPLICATION_JSON_VALUE))
						.andExpect(MockMvcResultMatchers.status().is(UNPROCESSABLE_ENTITY.value()))
				.andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
				.andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/product-composite/" + PRODUCT_ID_INVALID))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("INVALID: " + PRODUCT_ID_INVALID));
	}
	
	private ResultActions getAndVerifyProduct(int productId, HttpStatus expectedStatus) throws Exception{
		return mockMvc.perform(MockMvcRequestBuilders.get("/product-composite/"+ productId))
				.andExpect(MockMvcResultMatchers.status().is(expectedStatus.value()))
				.andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE));

	}
	
	private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/product-composite")
				.accept(APPLICATION_JSON_VALUE))
				.andExpect(MockMvcResultMatchers.status().is(expectedStatus.value()));
	}
	
	
	private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus) throws Exception{
		mockMvc.perform(MockMvcRequestBuilders.post("/product-composite")
						.content(jsonAsString(compositeProduct))
				.contentType(APPLICATION_JSON_VALUE)
				.accept(APPLICATION_JSON_VALUE))
						.andExpect(MockMvcResultMatchers.status().is(expectedStatus.value()));
	}

	private String jsonAsString(ProductAggregate compositeProduct) throws Exception{
		ObjectMapper om = new ObjectMapper();
		return  om.writeValueAsString(compositeProduct);
	}
}
