package se.magnus.microservices.core.review;

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
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;
import se.magnus.microservices.core.review.persistence.ReviewRepository;
import se.magnus.util.exceptions.InvalidInputException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:review-db", "eureka.client.enabled=false"}
)
public class AsyncReviewServiceImplTest {

    @Autowired
    ReviewRepository repository;

    @Autowired
    WebTestClient client;

    @Autowired
    private Sink channels;

    private AbstractMessageChannel input = null;

    @BeforeEach
    public void setupDb(){
        input = (AbstractMessageChannel) channels.input();
        repository.deleteAll();
    }

    @Test
    public void getReviewsByProductId(){
        int productId = 1;
        assertEquals(0, repository.findByProductId(productId).size());
        sendCreateReviewEvent(productId, 1);
        sendCreateReviewEvent(productId, 2);
        sendCreateReviewEvent(productId, 3);
        assertEquals(3, repository.findByProductId(productId).size());
        getAndverifyReviewsByProductId(productId, HttpStatus.OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].productId").isEqualTo(productId)
                .jsonPath("$[2].reviewId").isEqualTo(3);
    }

    @Test
    public void dupliateError(){
        int productId = 1;
        assertEquals(0, repository.findByProductId(productId).size());
        sendCreateReviewEvent(productId, 1);
        assertEquals(1, repository.findByProductId(productId).size());
        try {
            sendCreateReviewEvent(productId, 1);
            fail("Expecting Duplicate Exception error");
        } catch(MessagingException me){
            if (me.getCause() instanceof InvalidInputException){
                InvalidInputException iie = (InvalidInputException) me.getCause() ;
                assertEquals("Duplicate Key, Product Id: " + productId, iie.getMessage());
             }else {
                fail("Expecting Duplicate Exception Error but received " + me.getCause());
            }
        }

    }

    private WebTestClient.BodyContentSpec getAndverifyReviewsByProductId(int productId, HttpStatus expectedStatus){
        return getAndverifyReviewsByProductId("?productId="+productId, expectedStatus);
    }
    private WebTestClient.BodyContentSpec getAndverifyReviewsByProductId(String reviewPath, HttpStatus expectedStatus){
        return client.get().uri("/async/reviews"+ reviewPath).accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody();
    }

    public void sendCreateReviewEvent(int productId, int reviewId){
        Review review = new Review(productId, reviewId, "Author","Subject","Content","SA");
        Event<Integer, Review> event = new Event<>(Event.Type.CREATE, productId, review);
        input.send(new GenericMessage<>(event));
    }

    public void sendDeleteReviewEvent(int productId){
        Event<Integer, Review> event = new Event<>(Event.Type.DELETE, productId, null);
        input.send(new GenericMessage<>(event));
    }
}
