package se.magnus.microservices.core.product.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import se.magnus.api.core.product.AsyncProductService;
import se.magnus.api.core.product.Product;
import se.magnus.api.event.Event;
import se.magnus.util.exceptions.EventProcessingException;

@Slf4j
@EnableBinding(Sink.class)
public class ProductMessageProcessor {
    private final AsyncProductService productService;

    public ProductMessageProcessor(AsyncProductService productService) {
        this.productService = productService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process(Event<Integer, Product> event){
        log.info("Process message created at {}", event.getEventCreatedAt());

        switch (event.getEventType()){
            case CREATE:
                Product product = event.getData();
                log.info("Create Product with Id {} ", product.getProductId() );
                productService.createProduct(product);
                break;
            case DELETE:
                log.info("Deleting product with Id {} ", event.getKey());
                productService.deleteProduct(event.getKey());
                break;
            default:
                String errorMessage = String.format("Incorrect event type %s, expected a CREATE or DELETE event ", event.getEventType());
                log.warn(errorMessage);
                throw new EventProcessingException(errorMessage);
        }

        log.info("Message processing done!!");

    }
}
