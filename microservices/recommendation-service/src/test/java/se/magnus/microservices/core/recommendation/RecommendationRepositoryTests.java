package se.magnus.microservices.core.recommendation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
@DataMongoTest
@Slf4j
public class RecommendationRepositoryTests {

	RecommendationEntity savedEntity;
	
	@Autowired
	private RecommendationRepository repository;
	
	@BeforeEach
	public void setUpDb() {
		repository.deleteAll();
		
		RecommendationEntity entity = new RecommendationEntity(1, 1, "Author", 5, "Content");
		log.info("----->>>" + entity.toString());
		
		savedEntity = repository.save(entity);
		log.info("----->>>" + savedEntity.toString());
		
		assertEqualsRecommendation(entity, savedEntity);
	}

	@Test
	public void create() {
		RecommendationEntity entity = new RecommendationEntity(1, 2, "Author", 15, "Content1");
		repository.save(entity);
		
		RecommendationEntity foundEntity = repository.findById(entity.getId()).get();
		
		assertEqualsRecommendation(entity, foundEntity);
		
		assertEquals(2, repository.count());
	}
	
	@Test
	public void update() {
		savedEntity.setAuthor("Vikas");
		repository.save(savedEntity);
		
		RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).get();
		
		assertEqualsRecommendation(savedEntity, foundEntity);
		
		assertEquals(1, savedEntity.getVersion());
		assertEquals("Vikas", foundEntity.getAuthor());
		
	}
	
	@Test
	public void delete() {
		repository.delete(savedEntity);
		assertFalse(repository.existsById(savedEntity.getId()));
	}
	
	@Test
	private void getByProductId() {
		List<RecommendationEntity> foundEntities = repository.findByProductId(savedEntity.getProductId());
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
