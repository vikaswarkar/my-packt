package se.magnus.microservices.springcloud.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@Slf4j
public class HealthCheckConfig  {

    @Bean
    public HealthIndicator productCompositeHealthIndicator(){
        return new AbstractHealthIndicator() {
            @Override
            protected void doHealthCheck(Health.Builder builder) throws Exception {
                builder.up().build();
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
