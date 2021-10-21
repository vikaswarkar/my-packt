package se.magnus.microservices.composite.product.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@NoArgsConstructor
public class AppConfiguration {

    @Value("${app.product-service.url}")
    String productServiceUrl;

    @Value("${app.recommendation-service.url}")
    String recommendationServiceUrl;

    @Value("${app.review-service.url}")
    String reviewServiceUrl;

}
