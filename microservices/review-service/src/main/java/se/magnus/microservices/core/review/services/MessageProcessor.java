package se.magnus.microservices.core.review.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import se.magnus.api.core.review.AsyncReviewService;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;
import se.magnus.util.exceptions.EventProcessingException;

@Slf4j
@EnableBinding(Sink.class)
public class MessageProcessor {
    private final AsyncReviewService asyncReviewService;

    public MessageProcessor(AsyncReviewService asyncReviewService) {
        this.asyncReviewService = asyncReviewService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process(Event<Integer, Review> event){
        log.info("Process message created at {}", event.getEventCreatedAt());

        switch (event.getEventType()){
            case CREATE:
                Review review = event.getData();
                log.info("Create Product with Id {} ", review.getProductId() );
                asyncReviewService.createReview(review);
                break;
            case DELETE:
                log.info("Deleting review with Id {} ", event.getKey());
                asyncReviewService.deleteReviews(event.getKey());
                break;
            default:
                String errorMessage = String.format("Incorrect event type %s, expected a CREATE or DELETE event ", event.getEventType());
                log.warn(errorMessage);
                throw new EventProcessingException(errorMessage);
        }

        log.info("Message processing done!!");

    }
}
