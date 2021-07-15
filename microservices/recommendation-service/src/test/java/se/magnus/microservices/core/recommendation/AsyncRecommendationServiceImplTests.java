package se.magnus.microservices.core.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.recommendation.AsyncRecommendationService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.event.Event;
import se.magnus.microservices.core.recommendation.persistence.ReactiveRecommendationRepository;
import se.magnus.util.exceptions.InvalidInputException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.data.mongodb.port:0", "eureka.client.enabled=false"})
@ExtendWith(SpringExtension.class)
public class AsyncRecommendationServiceImplTests {

    @Autowired
    private AsyncRecommendationService recommendationService;

    @Autowired
    private ReactiveRecommendationRepository repository;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private Sink channels;

    private AbstractMessageChannel input = null;

    @BeforeEach
    public void setUpDb(){
        input = (AbstractMessageChannel) channels.input();
        repository.deleteAll().block();
    }

    @Test
    public void getRecommendationsByProductId(){
        int productId = 1;
        assertEquals(0, repository.count().block());
        sendRecommendationCreateEvent(productId, 1);
        sendRecommendationCreateEvent(productId, 2);
        sendRecommendationCreateEvent(productId, 3);
        assertEquals(3, repository.findByProductId(productId).count().block());
        getAndVerifyRecommendationsByProductId(productId, HttpStatus.OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].productId").isEqualTo(productId)
                .jsonPath("$[2].recommendationId").isEqualTo(3);
    }

    @Test
    public void duplicateError(){
        int productId = 1;
        assertEquals(0, repository.findByProductId(productId).count().block());
        sendRecommendationCreateEvent(productId, 1);
        assertEquals(1, repository.findByProductId(productId).count().block());

        try{
            sendRecommendationCreateEvent(productId, 1);
            fail("Expected a Messaging exception");
        }catch(MessagingException mex){
            if (mex.getCause() instanceof InvalidInputException){
                InvalidInputException iie = (InvalidInputException) mex.getCause();
                assertEquals("Duplicate Key, Product Id: 1, Recommendation Id 1", iie.getMessage());
            }else {
                fail("Expecting Invalid Inpuit Exception");
            }
        }
    }

    private void sendRecommendationCreateEvent(int productId, int recommendationId){
        Recommendation recommendation = new Recommendation(productId, recommendationId,
                "Author: " + recommendationId, 1, "Content: " + recommendationId, "SA");
        Event<Integer, Recommendation> event = new Event<>(Event.Type.CREATE, productId, recommendation);
        input.send(new GenericMessage<>(event));
    }

    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId, HttpStatus httpStatus){
        return getAndVerifyRecommendationsByProductId("?productId="+ productId, httpStatus);
    }
    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery, HttpStatus httpStatus) {
        return webTestClient.get().uri("/async/recommendations" + productIdQuery)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .expectBody();
    }

    @Test
    public void deleteRecommendations(){
        int productId = 1;
        assertEquals(0, repository.findByProductId(productId).count().block());
        sendRecommendationCreateEvent(productId, 1);
        sendRecommendationCreateEvent(productId, 2);
        sendRecommendationCreateEvent(productId, 3);
        assertEquals(3, repository.findByProductId(productId).count().block());
        sendDeleteRecommendationEvent(productId);
        assertEquals(0, repository.findByProductId(productId).count().block());
    }

    @Test
    public void getRecommendationMissingParameter(){
        getAndVerifyRecommendationsByProductId("", HttpStatus.BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/async/recommendations");
    }

    @Test
    public void getRecommendationsInvalidParameter(){
        getAndVerifyRecommendationsByProductId("?productId=non-number", HttpStatus.BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/async/recommendations");
    }

    private void sendDeleteRecommendationEvent(int productId){
        Event<Integer, Recommendation> event = new Event<>(Event.Type.DELETE, productId, null);
        input.send(new GenericMessage<>(event));
    }

}
