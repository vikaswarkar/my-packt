package se.magnus.microservices.composite.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;
import se.magnus.microservices.composite.product.services.async.AsyncProductCompositeIntegration;

import java.util.concurrent.BlockingQueue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;
import static se.magnus.microservices.composite.product.IsSameEvent.sameEventExceptCreatedAt;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"eureka.client.enabled=false"})
public class MessagingTests {

    private static final int PRODUCT_ID_OK = 1;
    private static final int PRODUCT_ID_NOT_FOUND = 2;
    private static final int PRODUCT_ID_INVALID =  3;

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    private MessageCollector messageCollector;

    @Autowired
    private AsyncProductCompositeIntegration.MessageSources channels;

    BlockingQueue<Message<?>> productsQueue = null;
    BlockingQueue<Message<?>> reviewsQueue = null;
    BlockingQueue<Message<?>> recommendationsQueue = null;

    @BeforeAll
    public static void testLevelSetup(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
    }

    @BeforeEach
    public void setUp() {
        productsQueue = getQueue(channels.outputProducts());
        reviewsQueue = getQueue(channels.outputReviews());
        recommendationsQueue = getQueue(channels.outputRecommendations());

        productsQueue.clear();
        reviewsQueue.clear();
        recommendationsQueue.clear();
    }

    private BlockingQueue<Message<?>> getQueue(MessageChannel messageChannel){
        return messageCollector.forChannel(messageChannel);
    }

    @Test
    public void createCompositeProduct1(){
        ProductAggregate compProduct = new ProductAggregate(1, "name", 1,null,null,null);
        postAndVerify(compProduct, HttpStatus.OK);
        assertEquals(1, productsQueue.size());
        Event<Integer, Product> productEvent = new Event<>(Event.Type.CREATE, compProduct.getProductId(),
                new Product(compProduct.getProductId(), compProduct.getName(), compProduct.getWeight(),null) );
        assertThat(productsQueue, receivesPayloadThat(sameEventExceptCreatedAt(productEvent)));
        assertEquals(0, recommendationsQueue.size());
        assertEquals(0, reviewsQueue.size());
    }

    private void postAndVerify(ProductAggregate compositeProduct, HttpStatus httpStatus){
        webTestClient.post()
                .uri("/async/product-composite")
                .body(Mono.just(compositeProduct), ProductAggregate.class)
                .exchange()
                .expectStatus().isEqualTo(httpStatus);
    }

    @Test
    public void deleteCompositeProduct(){
        deleteAndVerify(1, HttpStatus.OK);

        //Assert that one product event queued up.
        assertEquals(1, productsQueue.size());

        Event<Integer, Product> expectedProductEvent = new Event<>(Event.Type.DELETE, 1, null);
        assertThat(productsQueue, is(receivesPayloadThat(sameEventExceptCreatedAt(expectedProductEvent))));

        //Assert that one Review event queued up.
        assertEquals(1, reviewsQueue.size());

        Event<Integer, Review> expectedReviewEvent = new Event<>(Event.Type.DELETE, 1, null);
        assertThat(reviewsQueue, is(receivesPayloadThat(sameEventExceptCreatedAt(expectedReviewEvent))));

        //Assert that one Recommendation event queued up.
        assertEquals(1, recommendationsQueue.size());

        Event<Integer, Review> expectedRecommendationEvent = new Event<>(Event.Type.DELETE, 1, null);
        assertThat(recommendationsQueue, is(receivesPayloadThat(sameEventExceptCreatedAt(expectedRecommendationEvent))));

    }

    private void deleteAndVerify(int productId, HttpStatus httpStatus){
        webTestClient.delete().uri("/async/product-composite/"+productId)
                .exchange()
                .expectStatus().isEqualTo(httpStatus);
    }
}
