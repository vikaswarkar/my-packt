package se.magnus.microservices.core.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"spring.data.mongodb.port:0", "eureka.client.enabled=false"})
public class RecommendationServiceImplTests {

    private String RECOMMENDATION_BASE_URL = "/recommendations";

    MockMvc mockMvc;

    @Autowired
    RecommendationRepository repository;

    @Autowired
    WebApplicationContext webApplicationContext;

    @BeforeEach
    public void setupDb() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        repository.deleteAll();
    }

    @Test
    public void getRecommendationsByProductId() throws Exception {

        int productId = 1;

        postAndVerifyRecommendation(productId, 1, HttpStatus.OK);
        postAndVerifyRecommendation(productId, 2, HttpStatus.OK);
        postAndVerifyRecommendation(productId, 3, HttpStatus.OK);
        System.out.println("Response Is ===>> " +
                mockMvc.perform(MockMvcRequestBuilders.get(RECOMMENDATION_BASE_URL + "?productId=" + productId)
                                .content(APPLICATION_JSON_VALUE)
                                .accept(APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
                        .andExpect(MockMvcResultMatchers.jsonPath("$[0].productId").value(productId))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    @Test
    public void getRecommendationsMissingParameter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(RECOMMENDATION_BASE_URL)
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value("/recommendations"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").
                        value("Required request parameter 'productId' for method parameter type int is not present"));
    }

    @Test
    public void getRecommendationsInvalidParameter() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(RECOMMENDATION_BASE_URL + "?productId=no-integer")
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(RECOMMENDATION_BASE_URL));
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Type mismatch."));
    }

    @Test
    public void getRecommendationsNotFound() throws Exception {
        int productIdNotFound = 113;
        mockMvc.perform(MockMvcRequestBuilders.get(RECOMMENDATION_BASE_URL + "?productId=" + productIdNotFound)
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
    }

    @Test
    public void getRecommendationsInvalidParameterNegativeValue() throws Exception {
        int productIdInvalid = -1;
        mockMvc.perform(MockMvcRequestBuilders.get(RECOMMENDATION_BASE_URL + "?productId=" + productIdInvalid)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.UNPROCESSABLE_ENTITY.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(RECOMMENDATION_BASE_URL))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid productId: " + productIdInvalid));

    }

    private ResultActions postAndVerifyRecommendation(int productId, int recommendationId, HttpStatus expectedStatus) throws Exception {
        Recommendation recommendation = new Recommendation(productId, recommendationId, "Author", 4, "Contect", "SA");

        return mockMvc.perform(MockMvcRequestBuilders.post(RECOMMENDATION_BASE_URL + "?productId=" + productId)
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON)
                        .content(getJsonString(recommendation)))
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus.value()))
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE));
    }

    private String getJsonString(Recommendation recommendation) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        return om.writeValueAsString(recommendation);
    }
}
