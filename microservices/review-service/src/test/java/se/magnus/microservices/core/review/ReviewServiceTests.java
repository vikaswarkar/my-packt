package se.magnus.microservices.core.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.core.review.persistence.ReviewRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:review-db",
                "eureka.client.enabled=false"})
@Slf4j
public class ReviewServiceTests {

    private TestRestTemplate testRestTemplate = new TestRestTemplate();

    @Autowired
    WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    ReviewRepository repository;

    @LocalServerPort
    private String portNumber;

    private String REVIEW_BASE_URL = "";

    private String REVIEW_BASE_URI = "/reviews";

    HttpHeaders httpHeaders = new HttpHeaders();

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        REVIEW_BASE_URL = String.format("http://localhost:%s%s", portNumber, REVIEW_BASE_URI);
        repository.deleteAll();
    }

    @Test
    public void getReviewsByProductId() throws Exception {
        assertEquals(0, repository.count());

        postAndVerifyReview(1, 1, HttpStatus.OK);
        postAndVerifyReview(1, 2, HttpStatus.OK);
        postAndVerifyReview(1, 3, HttpStatus.OK);

        assertEquals(3, repository.count());

        int productId = 1;
//		printJson(productId);

        String json = getAndVerifyReviewsByProductIdTestRestTemplate(productId, HttpStatus.OK);
        List<Review> reviewList = getReviewList(json);
        assertThat(reviewList.size()).isEqualTo(3);
    }

    private List<Review> getReviewList(String json) throws Exception {
        ObjectMapper om = new ObjectMapper();

        JSONArray jsonArray = new JSONArray(json);
        List<Review> reviewList = new ArrayList<>();
        Review review;
        for (int i = 0; i < jsonArray.length(); i++) {
            review = om.readValue(jsonArray.getString(i), Review.class);
            reviewList.add(review);
        }
        return reviewList;
    }

//	private void printJson(int productId) {
//		WebClient webClient = WebClient.create();
//
//		log.info("JSON ==>"+ webClient.get().uri("http://localhost:"+portNumber + REVIEW_BASE_URL + "?productId="+productId).exchange()
//				.block().bodyToMono(String.class).block());
//
//	}

    @Test
    public void duplicateError() throws Exception {
        int productId = 1, reviewId = 1;

        postAndVerifyReview(productId, reviewId, HttpStatus.OK)
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.reviewId").value(reviewId));

        assertEquals(1, repository.count());

        postAndVerifyReview(productId, reviewId, HttpStatus.UNPROCESSABLE_ENTITY)
                .andExpect(jsonPath("$.path").value(REVIEW_BASE_URI))
                .andExpect(jsonPath("$.message")
                        .value("Duplicate Key, Product Id: 1, Review Id: 1"));
    }

    @Test
    public void deleteReviews() throws Exception {
        int productId = 1;
        int reviewId = 1;
        postAndVerifyReview(productId, reviewId, HttpStatus.OK);
        assertEquals(1, repository.count());

        deleteAndVerifyReviewsByProductId(1, HttpStatus.OK);
        assertEquals(0, repository.count());

        deleteAndVerifyReviewsByProductId(1, HttpStatus.OK);
        assertEquals(0, repository.count());
    }

    @Test
    public void getReviewsMissingParameter() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders
                        .get(REVIEW_BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON)
                )
                .andExpect(status().is(BAD_REQUEST.value()))
                .andExpect(header().string("Content-Type", APPLICATION_JSON.toString()))
                .andExpect(jsonPath("$.path").value("/reviews"))
                .andExpect(jsonPath("$.message").value("Required request parameter 'productId' for method parameter type int is not present"))
        ;
    }

    @Test
    public void getReviewsInvalidParameter() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get(REVIEW_BASE_URL + "?productId=no-integer")
                        .accept(APPLICATION_JSON))
                .andExpect(status().is(BAD_REQUEST.value()));

    }

    @Test
    public void getReviewsNotFound() throws Exception {
        int productIdNotFound = 213;
        mockMvc.perform(MockMvcRequestBuilders.get(REVIEW_BASE_URL + "?productId=" + productIdNotFound)
                        .accept(APPLICATION_JSON)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getReviewsInvalidParameterNegativeValue() throws Exception {

        int productIdInvalid = -1;
        mockMvc.perform(MockMvcRequestBuilders.get(REVIEW_BASE_URL + "?productId=" + productIdInvalid)
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON))
                .andExpect(status().is(UNPROCESSABLE_ENTITY.value()))
                .andExpect(jsonPath("$.path").value(REVIEW_BASE_URI))
                .andExpect(jsonPath("$.message").value("Invalid productId: " + productIdInvalid));
    }

    private String getAndVerifyReviewsByProductIdTestRestTemplate(int productId, HttpStatus expectedStatus) {
        ResponseEntity<String> response = testRestTemplate.exchange(REVIEW_BASE_URL + "?productId=" + productId, HttpMethod.GET, null, String.class);
        assertEquals(expectedStatus.value(), response.getStatusCodeValue());
        assertEquals(APPLICATION_JSON, response.getHeaders().getContentType());
        return response.getBody();
    }

    private ResultActions getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) throws Exception {
        return this.mockMvc.perform(MockMvcRequestBuilders
                .get(REVIEW_BASE_URL)
                .accept(APPLICATION_JSON)
        ).andExpect(status().is(expectedStatus.value()));
    }


    private String postAndVerifyReview1(int productId, int reviewId, HttpStatus expectedStatus) {
        Review review = new Review(productId, reviewId, "Author" + reviewId, "Subject" + reviewId, "Content" + reviewId, "SA");
        HttpEntity<Review> httpEntity = new HttpEntity<>(review, httpHeaders);

        ResponseEntity<String> response = testRestTemplate.exchange(REVIEW_BASE_URL, HttpMethod.POST, httpEntity, String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(response.getHeaders().getContentType(), APPLICATION_JSON);
        return response.getBody();
    }

    private ResultActions postAndVerifyReview(int productId, int reviewId, HttpStatus expectedStatus) throws Exception {
        Review review = new Review(productId, reviewId, "Author" + reviewId, "Subject" + reviewId, "Content" + reviewId, "SA");
        return this.mockMvc.perform(MockMvcRequestBuilders
                        .post(REVIEW_BASE_URL)
                        .content(asJsonString(review))
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON)
                )
                .andExpect(status().is(expectedStatus.value()));
    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
        ResponseEntity<String> response = testRestTemplate.exchange(REVIEW_BASE_URL + "?productId=" + productId, HttpMethod.DELETE, null, String.class);
        assertEquals(expectedStatus.value(), response.getStatusCodeValue());
    }
}
