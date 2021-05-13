package se.magnus.microservices.core.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import se.magnus.microservices.core.review.persistence.ReviewEntity;
import se.magnus.microservices.core.review.persistence.ReviewRepository;
import se.magnus.microservices.core.review.services.ReviewMapper;
import se.magnus.util.http.ServiceUtil;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Slf4j
public class PersistenceTests {

	private ReviewRepository repository;
	private ReviewMapper mapper;
	private ServiceUtil serviceUtil;
	private ReviewEntity entity1;
	private ReviewEntity savedEntity;
	
	@Autowired
	public PersistenceTests(ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil) {
		this.repository = repository;
		this.mapper = mapper;
		this.serviceUtil = serviceUtil;
		
	}
	
	@BeforeEach
	public void setUp() {
		repository.deleteAll();
		entity1 = new ReviewEntity(1, 1, "Author", "Subject", "Content");
		savedEntity = repository.save(entity1);
		assertEqualsReview(entity1, savedEntity);
	}
	
	@Test
	public void create() {
		
		ReviewEntity entity2 = new ReviewEntity(1, 2, "Author", "Subject", "Content");
		entity2 = repository.save(entity2);
		ReviewEntity foundEntity = repository.findById(entity2.getId()).get();
		assertEqualsReview(entity2, foundEntity);
		assertEquals(2,repository.count());
	}

	@Test
	public void update() {
		entity1.setAuthor("DifferentAuthor");
		repository.save(entity1);
		ReviewEntity updatedEntity = repository.findById(entity1.getId()).get();
		assertEqualsReview(entity1, updatedEntity);
		assertEquals("DifferentAuthor", updatedEntity.getAuthor());
//		assertEquals(1, (long)updatedEntity.getVersion());
	}
	@Test
	public void delete() {
		repository.delete(entity1);
		assertFalse(repository.findById(entity1.getId()).isPresent());
	}
	
	@Test
	public void getProductById() {
		List<ReviewEntity> reviewEntities = repository.findByProductId(entity1.getProductId());
//		assertThat(reviewEntities, hasSize(1));
		assertEquals(1, reviewEntities.size());
	}
	
	@Test
	public void duplicateError() {
		log.info("savedEntity: {}",  savedEntity.toString());
		
		ReviewEntity duplicateEntity = new ReviewEntity(1, 1, "Author1", "Subject1", "Content1");
		
		assertThrows(DataIntegrityViolationException.class, ()-> repository.save(duplicateEntity));
		
		log.info("duplicateEntity: {}", duplicateEntity.toString());
	}
	
	private void assertEqualsReview(ReviewEntity entity, ReviewEntity savedEntity) {
		assertEquals(entity.getProductId(), savedEntity.getProductId());
		assertEquals(entity.getReviewId(), savedEntity.getReviewId());
		assertEquals(entity.getAuthor(), savedEntity.getAuthor());
		assertEquals(entity.getSubject(), savedEntity.getSubject());
		assertEquals(entity.getContent(), savedEntity.getContent());
		assertNotNull(savedEntity.getId());
	}
	
}
