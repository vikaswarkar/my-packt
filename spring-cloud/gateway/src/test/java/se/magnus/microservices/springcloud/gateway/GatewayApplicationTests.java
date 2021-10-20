package se.magnus.microservices.springcloud.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {"eureka.client.enabled=false",
        "spring.main.allow-bean-definition-overriding=true"})
class GatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
