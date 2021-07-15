package se.magnus.microservices.core.product.services;

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
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;
import se.magnus.microservices.core.product.persistence.ReactiveProductRepository;
import se.magnus.util.exceptions.InvalidInputException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.data.mongodb.port:0", "eureka.client.enabled=false"})
class AsyncProductServiceImplTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ReactiveProductRepository repository;

    @Autowired
    private Sink channels;

    private AbstractMessageChannel input = null;

    @BeforeEach
    public void setUpDb(){
        input = (AbstractMessageChannel) channels.input();
        repository.deleteAll().block();
    }

    private void sendCreateProductEvent(int productId){
        Product product = new Product(productId, "Name: " + productId,productId, "SA" );
        Event<Integer, Product> event = new Event(Event.Type.CREATE, productId, product);
        input.send(new GenericMessage<>(event));
    }

    private void sendDeleteProductEvent(Integer productId){
        Event<Integer, Product> event = new Event<>(Event.Type.DELETE, productId, null);
        input.send(new GenericMessage<>(event));
    }

    @Test
    public void getProductById(){
        int productId = 1;

        assertNull(repository.findByProductId(productId).block());
        assertEquals(0, repository.count().block());

        sendCreateProductEvent(productId);

        assertNotNull(repository.findByProductId(productId).block());
        assertEquals(1, repository.count().block());

        getAndVerifyProduct(productId, HttpStatus.OK)
        .jsonPath("$.productId").isEqualTo(productId);
    }

    @Test
    public void duplicateError(){
        int productId = 2;
        assertNull(repository.findByProductId(productId).block());

        sendCreateProductEvent(productId);
        assertNotNull(repository.findByProductId(productId).block());

        try{
            sendCreateProductEvent(productId);
            fail("Should have failed with and expected a Messaging Error");
        }catch(MessagingException me){
            if (me.getCause() instanceof InvalidInputException){
                InvalidInputException iie = (InvalidInputException) me.getCause();
                assertEquals("Duplicate Key, ProductId: " + productId, iie.getMessage());
            }else {
                fail("Expected a InvalidInputException as the root cause");
            }
        }
    }

    @Test
    public void getProductInvalidParameterString(){
        getAndVerifyProduct("/async/product/no-integer", HttpStatus.BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/async/product/no-integer");
    }

    @Test
    public void getProductNotFound(){
        int productId = 7;
        getAndVerifyProduct(productId, HttpStatus.NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/async/product/"+productId)
                .jsonPath("$.message").isEqualTo("No product found for productId " + productId );
    }

    @Test
    public void getProductInvalidParameterNegativeValue(){
        int productId = -1;
        getAndVerifyProduct(productId,HttpStatus.UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/async/product/"+productId)
                .jsonPath("$.message").isEqualTo("Invalid Product Id: " + productId);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(Integer productId, HttpStatus httpStatus){
        return getAndVerifyProduct("/async/product/"+productId, httpStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(String path, HttpStatus httpStatus){
        return webTestClient.get().uri(path)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .exchange()
                .expectStatus().isEqualTo(httpStatus)
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBody();
    }

    @Test
    public void deleteProduct(){
        int productId = 5;
        getAndVerifyProduct(5, HttpStatus.NOT_FOUND);
        sendCreateProductEvent(productId);
        getAndVerifyProduct(5, HttpStatus.OK);

        sendDeleteProductEvent(productId);
        getAndVerifyProduct(5, HttpStatus.NOT_FOUND);
    }
}