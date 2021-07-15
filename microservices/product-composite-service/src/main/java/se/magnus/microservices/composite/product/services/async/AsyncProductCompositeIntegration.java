package se.magnus.microservices.composite.product.services.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.AsyncProductService;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.AsyncRecommendationService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.AsyncReviewService;
import se.magnus.api.core.review.Review;
import se.magnus.api.event.Event;
import se.magnus.microservices.composite.product.services.ExceptionHelper;

@Component
@Slf4j
@EnableBinding(AsyncProductCompositeIntegration.MessageSources.class)
public class AsyncProductCompositeIntegration implements AsyncProductService, AsyncRecommendationService,
		AsyncReviewService {

    private WebClient webClient;
    private final WebClient.Builder webClientBuilder;
	private final ObjectMapper mapper;

	private final String productServiceUrl = "http://product/reactive/product/";
	private final String recommendationServiceUrl = "http://recommendation/reactive/recommendation?productId=";
	private final String reviewServiceUrl = "http://review/reactive/review?productId=";

	private ExceptionHelper exceptionHelper;
    private MessageSources messageSources;

	public interface MessageSources{
		String OUTPUT_PRODUCTS = "output-products";
		String OUTPUT_RECOMMENDATIONS = "output-recommendations";
		String OUTPUT_REVIEWS = "output-reviews";

		@Output(OUTPUT_PRODUCTS)
		MessageChannel outputProducts();

		@Output(OUTPUT_REVIEWS)
		MessageChannel outputReviews();

		@Output(OUTPUT_RECOMMENDATIONS)
		MessageChannel outputRecommendations();
	}

	@Autowired
    public AsyncProductCompositeIntegration(
        WebClient.Builder webClientBuilder,
		ObjectMapper mapper,
        MessageSources messageSources,
        ExceptionHelper exceptionHelper
//		,@Value("${app.product-service.host}") String productServiceHost,
//        @Value("${app.product-service.port}") int    productServicePort,
//        @Value("${app.recommendation-service.host}") String recommendationServiceHost,
//        @Value("${app.recommendation-service.port}") int    recommendationServicePort,
//        @Value("${app.review-service.host}") String reviewServiceHost,
//        @Value("${app.review-service.port}") int    reviewServicePort
	) {

//        this.webClient = webClientBuilder.build();
		this.webClientBuilder = webClientBuilder;
        this.mapper = mapper;
        this.exceptionHelper = exceptionHelper;
        this.messageSources = messageSources;

//        productServiceUrl        = "http://" + productServiceHost + ":" + productServicePort + "/reactive/product/";
//        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/reactive/recommendation?productId=";
//        reviewServiceUrl         = "http://" + reviewServiceHost + ":" + reviewServicePort + "/reactive/review?productId=";
    }

	@Override
	public Product createProduct(Product body) {
		messageSources.outputProducts().send(
				MessageBuilder.withPayload(
						new Event(Event.Type.CREATE, body.getProductId(), body)
				).build() ) ;
		return body;
	}
	
	@Override
    public Mono<Product> getProduct(int productId) {
            String url = productServiceUrl + productId;
            log.debug("Will call getProduct API on URL: {}", url);

            return getWebClient().get().uri(url).retrieve().bodyToMono(Product.class).log()
					.onErrorMap(WebClientResponseException.class, ex-> handleException(ex));
    }
    
	@Override
	public void deleteProduct(int productId) {
		messageSources.outputProducts().send(MessageBuilder.withPayload(
				new Event(Event.Type.DELETE, productId, null)).build());
	}

	@Override
	public Recommendation createRecommendation(Recommendation body) {
		messageSources.outputRecommendations().send(
				MessageBuilder.withPayload(
						new Event<>(Event.Type.CREATE, body.getProductId(), body)
				).build()
		);
		return body;
	}

	@Override
    public Flux<Recommendation> getRecommendations(int productId) {
		String url = recommendationServiceUrl + productId;
		log.debug("Will call getRecommendations API on URL: {}", url);
		return getWebClient().get().uri(url)
				.retrieve()
				.bodyToFlux(Recommendation.class)
				.log();
    }

	@Override
	public void deleteRecommendations(int productId) {
		messageSources.outputRecommendations().send(
				MessageBuilder.withPayload(
						new Event<>(Event.Type.DELETE, productId, null)
				).build()
		);
	}

	@Override
	public Review createReview(Review body) {
		messageSources.outputReviews().send(
				MessageBuilder.withPayload(
						new Event<>(Event.Type.CREATE, body.getProductId(), body)
				).build()
		);
		return body;
	}

	@Override
    public Flux<Review> getReviews(int productId) {
		String url = reviewServiceUrl + productId;

		log.debug("Will call getReviews API on URL: {}", url);
		return getWebClient().get().uri(url).retrieve().bodyToFlux(Review.class).log();
}

	@Override
	public void deleteReviews(int productId) {
		messageSources.outputReviews().send(
				MessageBuilder.withPayload(
						new Event<>(Event.Type.DELETE, productId, null)
				).build()
		);
	}

	private Mono<Health> getProductHealth(){
		return getHealth(productServiceUrl);
	}

	private Mono<Health> getRecommendationHealth(){
		return getHealth(recommendationServiceUrl);
	}

	private Mono<Health> getReviewHealth(){
		return getHealth(reviewServiceUrl);
	}

	private Mono<Health> getHealth(String url){
		url += "/actuator/health";
		log.info("Calling actuator on {}", url);
		return getWebClient().get().uri(url).retrieve().bodyToMono(String.class)
				.map(s -> new Health.Builder().up().build())
				.onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
				.log();
	}

	private WebClient getWebClient(){
		if (webClient == null){
			webClient = webClientBuilder.build();
		}
		return webClient;
	}

	private Throwable handleException(Throwable ex) {
		if (!(ex instanceof WebClientResponseException)){
			log.warn(String.format("Got unexpected error: %s. will rethrow it", ex.toString()));
			return ex;
		}
		WebClientResponseException wcre = (WebClientResponseException)ex;
		return exceptionHelper.handleException(wcre, wcre.getStatusCode());
	}
}
