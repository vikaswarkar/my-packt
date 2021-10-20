package se.magnus.microservices.composite.product.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@NoArgsConstructor
public class AppConfiguration {

    @Value("${app.product-service.host}")
    String productServiceHost;

    @Value("${app.product-service.port}")
    int productServicePort;

    @Value("${app.recommendation-service.host}")
    String recommendationServiceHost;

    @Value("${app.recommendation-service.port}")
    int recommendationServicePort;

    @Value("${app.review-service.host}")
    String reviewServiceHost;

    @Value("${app.review-service.port}")
    int reviewServicePort;
}
