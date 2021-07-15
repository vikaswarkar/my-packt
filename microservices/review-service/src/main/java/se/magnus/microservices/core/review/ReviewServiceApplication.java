package se.magnus.microservices.core.review;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;

@SpringBootApplication
@ComponentScan("se.magnus")
@Slf4j
public class ReviewServiceApplication {

	private final Integer connectionPoolSize;

	public ReviewServiceApplication(@Value("${spring.datasource.maximum-pool-size:10}") Integer connectionPoolSize) {
		this.connectionPoolSize = connectionPoolSize;
	}

	@Bean
	public Scheduler jdbcScheduler(){
		log.info(String.format("Creating a jdbcScheduler with connectionPoolSize: %s", connectionPoolSize));
		return Schedulers.fromExecutor(Executors.newFixedThreadPool(connectionPoolSize));
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(ReviewServiceApplication.class, args);
		String dbUrl = ctx.getEnvironment().getProperty("spring.datasource.url");
		log.info(String.format("Connected to %s", dbUrl));
	}

}
