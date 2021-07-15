package se.magnus.microservices.core.recommendation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import se.magnus.api.core.recommendation.AsyncRecommendationService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.event.Event;
import se.magnus.util.exceptions.EventProcessingException;

@Slf4j
@EnableBinding(Sink.class)
public class RecommendationMessageProcessor {
    private final AsyncRecommendationService recommendationService;

    public RecommendationMessageProcessor(AsyncRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process(Event<Integer, Recommendation> event){
        log.info("Process message created at {}", event.getEventCreatedAt());

        switch (event.getEventType()){
            case CREATE:
                Recommendation recommendation = event.getData();
                log.info("Create Product with Id {} ", recommendation.getProductId() );
                recommendationService.createRecommendation(recommendation);
                break;
            case DELETE:
                log.info("Deleting recommendation with Id {} ", event.getKey());
                recommendationService.deleteRecommendations(event.getKey());
                break;
            default:
                String errorMessage = String.format("Incorrect event type %s, expected a CREATE or DELETE event ", event.getEventType());
                log.warn(errorMessage);
                throw new EventProcessingException(errorMessage);
        }

        log.info("Message processing done!!");

    }
}
