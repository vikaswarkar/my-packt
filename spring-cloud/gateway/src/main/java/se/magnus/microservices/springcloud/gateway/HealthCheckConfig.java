package se.magnus.microservices.springcloud.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Configuration
@Slf4j
public class HealthCheckConfig  {

    @Autowired
    RestTemplate restTemplate;

    private final WebClient.Builder webClientBuilder;

    private WebClient webClient;

    HealthCheckConfig(WebClient.Builder webClientBuilder){
        this.webClientBuilder = webClientBuilder;
    }

    private WebClient getWebClient(){
        if (this.webClient == null){
            this.webClient = webClientBuilder.build();
        }
        return this.webClient;
    }

    @Bean
    public HealthIndicator testHealthIndicator(){
        return new AbstractHealthIndicator() {
            @Override
            protected void doHealthCheck(Health.Builder builder) throws Exception {
                builder.up().build();
            }
        };
    }

    private HealthIndicator getHealthIndicatorx(String url){
        url+= "/actuator/health";
        log.info("Calling health on URL {} ", url );
        String status = restTemplate.getForObject(url, String.class);
        return new AbstractHealthIndicator() {
            @Override
            protected void doHealthCheck(Health.Builder builder) throws Exception {
                builder.status(status).build();
            }
        };
    }

    private HealthIndicator getHealthIndicator(String url){
        url+= "/actuator/health";
        log.info("Calling health on URL {} ", url );
        Mono<Health> health = getWebClient().get().uri(url).retrieve().bodyToMono(String.class)
                .map(s -> new Health.Builder().up().build())
                .onErrorResume( ex  -> Mono.just(new Health.Builder().down().build()))
                .log();
        return new AbstractHealthIndicator() {
            @Override
            protected void doHealthCheck(Health.Builder builder) throws Exception {
                builder.status(health.block().getStatus()).build();
            }
        };
    }

    @Bean
    public CompositeHealthContributor productCompositeHealthIndicator(){
        return new CompositeHealthContributor() {
            Map<String, HealthContributor> contributors = new LinkedHashMap<>();
            {
                contributors.put("product", getHealthIndicator("http://product"));
                contributors.put("review", getHealthIndicator("http://review"));
                contributors.put("recommendation", getHealthIndicator("http://recommendation"));
            }
            @Override
            public HealthContributor getContributor(String name) {
                return contributors.get(name);
            }

            @Override
            public Iterator<NamedContributor<HealthContributor>> iterator() {
                return contributors.entrySet().stream()
                        .map((entry) -> NamedContributor.of(entry.getKey(), entry.getValue())).iterator();
            }
        };
    }

    @Bean
    public StatusAggregator productCompositeHealthAggregator(){
        return new StatusAggregator() {
            @Override
            public Status getAggregateStatus(Set<Status> statuses) {
                boolean statusUp = statuses.stream().allMatch(s -> s.equals(Status.UP));
                return statusUp ? Status.UP : Status.DOWN;
            }
        };
    }

}
