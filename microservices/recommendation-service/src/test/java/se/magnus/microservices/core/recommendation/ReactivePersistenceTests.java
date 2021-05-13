package se.magnus.microservices.core.recommendation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.extern.slf4j.Slf4j;
import se.magnus.microservices.core.recommendation.persistence.ReactiveRecommendationRepository;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;

@ExtendWith(SpringExtension.class)
@DataMongoTest
@Slf4j
public class ReactivePersistenceTests {

	RecommendationEntity savedEntity;
	
	@Autowired
	private ReactiveRecommendationRepository repository;
	
	@BeforeEach
	public void setUpDb() {
		repository.deleteAll();
		
		RecommendationEntity entity = new RecommendationEntity(1, 1, "Author", 5, "Content");
		log.info("----->>>" + entity.toString());
		
		savedEntity = repository.save(entity).block();
		log.info("----->>>" + savedEntity.toString());
		
		assertEqualsRecommendation(entity, savedEntity);
	}

	@Test
	public void create() {
		RecommendationEntity entity = new RecommendationEntity(1, 2, "Author", 15, "Content1");
		repository.save(entity).block();
		
		RecommendationEntity foundEntity = repository.findById(entity.getId()).block();
		
		assertEqualsRecommendation(entity, foundEntity);
		
		assertEquals(2, repository.count().block());
	}
	
	@Test
	public void update() {
		savedEntity.setAuthor("Vikas");
		repository.save(savedEntity).block();
		
		RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).block();
		
		assertEqualsRecommendation(savedEntity, foundEntity);
		
		assertEquals(1, savedEntity.getVersion());
		assertEquals("Vikas", foundEntity.getAuthor());
		
	}
	
	@Test
	public void delete() {
		repository.delete(savedEntity).block();
		assertFalse(repository.existsById(savedEntity.getId()).block());
	}
	
	@Test
	private void getByProductId() {
		List<RecommendationEntity> foundEntities = repository.findByProductId(savedEntity.getProductId()).collectList().block();
		assertThat(foundEntities, hasSize(1));
		assertEqualsRecommendation(savedEntity, foundEntities.get(0));
	}
	
	private void assertEqualsRecommendation(RecommendationEntity expected, RecommendationEntity actual) {
		assertEquals(expected.getProductId(), actual.getProductId());
		assertEquals(expected.getRecommendationId(), actual.getRecommendationId());
		assertEquals(expected.getId(), actual.getId());
		assertEquals(expected.getAuthor(), actual.getAuthor());
		assertEquals(expected.getContent(), actual.getContent());
	}
	
}
