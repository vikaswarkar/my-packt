package se.magnus.microservices.composite.product.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
//import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.composite.product.*;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);
//    private final SecurityContext securityContext = new SecurityContextImpl();
    private final ServiceUtil serviceUtil;
    private  ProductCompositeIntegration integration;

    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public void createCompositeProduct(ProductAggregate body) {
//        ReactiveSecurityContextHolder.getContext().doOnSuccess(sc -> createCompositeProductInternal(sc, body));
        createCompositeProductInternal(body);
    }

//    private void createCompositeProductInternal(SecurityContext sc, ProductAggregate body) {
    public void createCompositeProductInternal(ProductAggregate body) {
        try {
//            logAuthorizationInfo(sc);

            LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

            Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), serviceUtil.getServiceAddress());
            integration.createProduct(product);

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), serviceUtil.getServiceAddress());
                    integration.createRecommendation(recommendation);
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach(r -> {
                    Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), serviceUtil.getServiceAddress());
                    integration.createReview(review);
                });
            }
            LOG.debug("createCompositeProduct: composite entites created for productId: {}", body.getProductId());
        } catch (RuntimeException re) {
            LOG.warn("createCompositeProduct failed", re);
            throw re;
        }
    }

    @Override
    public ProductAggregate getCompositeProduct(int productId) {
        LOG.debug("getCompositeProduct: lookup a product aggregate for productId: {}", productId);

        Product product = integration.getProduct(productId);
        if (product == null) throw new NotFoundException("No product found for productId: " + productId);

        List<Recommendation> recommendations = integration.getRecommendations(productId);

        List<Review> reviews = integration.getReviews(productId);

        LOG.debug("getCompositeProduct: aggregate entity found for productId: {}", productId);

        return createProductAggregate(product, recommendations, reviews, serviceUtil.getServiceAddress());

//        return createProductAggregate(ReactiveSecurityContextHolder.getContext().defaultIfEmpty(securityContext).block(),
//                product, recommendations, reviews, serviceUtil.getServiceAddress());
    }

    @Override
    public void deleteCompositeProduct(int productId) {
//        ReactiveSecurityContextHolder.getContext().doOnSuccess(sc -> deleteCompositeProductInternal(sc, productId));
        deleteCompositeProductInternal(productId);
    }

    private void deleteCompositeProductInternal( int productId) {
        try{
//            logAuthorizationInfo(sc);
            LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

            integration.deleteProduct(productId);

            integration.deleteRecommendations(productId);

            integration.deleteReviews(productId);

            LOG.debug("getCompositeProduct: aggregate entities deleted for productId: {}", productId);
        } catch(RuntimeException rex){
            LOG.warn("deleteCompositeProduct failed: {}", rex.toString());
            throw rex;
        }

    }

    private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {
//        SecurityContext sc,
//        logAuthorizationInfo(sc);

        // 1. Setup product info
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        // 2. Copy summary recommendation info, if available
        List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
             recommendations.stream()
                .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
                .collect(Collectors.toList());

        // 3. Copy summary review info, if available
        List<ReviewSummary> reviewSummaries = (reviews == null)  ? null :
            reviews.stream()
                .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
                .collect(Collectors.toList());

        // 4. Create info regarding the involved microservices addresses
        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }

//    private void logAuthorizationInfo(SecurityContext sc){
//        if (sc!=null && sc.getAuthentication()!=null && sc.getAuthentication() instanceof JwtAuthenticationToken){
//            Jwt jwtToken = ((JwtAuthenticationToken) sc.getAuthentication()).getToken();
//            logAuthorizationInfo(jwtToken);
//        } else {
//            LOG.warn("No JWT based Authentication supplied, running tests??");
//        }
//    }
//
//    private void logAuthorizationInfo(Jwt jwt){
//        if (jwt == null){
//            LOG.warn("No JWT supplied, running tests??");
//        }else {
//            if (LOG.isDebugEnabled()){
//                URL issuer = jwt.getIssuer();
//                List<String> audience = jwt.getAudience();
//                Object subject = jwt.getClaims().get("sub");
//                Object scopes = jwt.getClaims().get("scope");
//                Object expires = jwt.getClaims().get("exp");
//
//                LOG.debug("Authorization info: Subject: {}, scopes: {}, expires: {}, issuer: {}, audience: {}",
//                        subject, scopes, expires, issuer, audience);
//            }
//        }
//    }
}