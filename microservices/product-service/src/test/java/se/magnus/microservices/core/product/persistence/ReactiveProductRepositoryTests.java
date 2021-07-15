package se.magnus.microservices.core.product.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@DataMongoTest
class ReactiveProductRepositoryTests {

	@Autowired
	ReactiveProductRepository repository;
	
	private ProductEntity savedEntity;
	
	@BeforeEach
	public void setUp() {
		System.out.println("Cleaning upp....................");
	}
	
	@BeforeEach
	public void setupDb() {
		System.out.println("Setting up db");
		
		StepVerifier.create(repository.deleteAll()).verifyComplete();
		
		ProductEntity entity = new ProductEntity(1, "n1", 1);
		
		StepVerifier.create(repository.save(entity))
			.expectNextMatches(createdEntity -> {
				this.savedEntity = createdEntity;
				return areProductEqual(entity, savedEntity);
			}).verifyComplete();
	}
	
	@AfterEach
	public void tearDown() {
		System.out.println("---->>>>>>>>Tearing down the test setup");
	}
	
	@Test
	void create() {
		ProductEntity newEntity = new ProductEntity(2, "n2", 2);
		StepVerifier.create(repository.save(newEntity))
			.expectNextMatches( entityCreated -> {
				this.savedEntity = entityCreated;
				return areProductEqual(newEntity, this.savedEntity);
			}).verifyComplete();
		
		StepVerifier.create(repository.count()).expectNext(2l).verifyComplete();
	}

	@Test
	public void update() {
		savedEntity.setName("n2");
		
		StepVerifier.create(repository.save(savedEntity)).expectNext(savedEntity).verifyComplete();
		
		StepVerifier.create(repository.findById(savedEntity.getId()))
			.expectNextMatches(foundEntity-> areProductEqual(savedEntity, foundEntity)).verifyComplete();
		
	}
	
	@Test
	public void delete() {
		StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
		StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();
	}
	
	@Test
	public void

	getByProductId(){
		StepVerifier.create(repository.findByProductId(1))
			.expectNextMatches(foundEntity-> areProductEqual(this.savedEntity, foundEntity));
	}
	
	@Test
	public void duplicateError() {
		ProductEntity duplicateProduct = new ProductEntity(savedEntity.getProductId(), "n1", 1);
		StepVerifier.create(repository.save(duplicateProduct)).expectError(DuplicateKeyException.class).verify();
	}
	
	@Test
	public void optimisticLockError() {
		assertEquals(0, savedEntity.getVersion());
		ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
		ProductEntity entity2 = repository.findById(savedEntity.getId()).block();
		
		entity1.setName("n1-1");
		repository.save(entity1).block();
		
		StepVerifier.create(repository.findById(savedEntity.getId())).expectNextMatches(en ->
			{
				assertEquals(1, en.getVersion());
				return (1 == en.getVersion());
			}	
		).verifyComplete();
		
		entity2.setName("n2-2");
		StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class);
		
		ProductEntity foundEntity = repository.findById(savedEntity.getId()).block();
		assertEquals(1, foundEntity.getVersion());
		assertEquals("n1-1", foundEntity.getName());
		
	}
	
	private boolean areProductEqual(ProductEntity expected, ProductEntity actual) {
		return (expected.getId().equals(actual.getId()) && 
		expected.getVersion() == actual.getVersion() &&
		expected.getProductId() == actual.getProductId() &&
		expected.getName().equals(actual.getName()) &&
		expected.getWeight() == actual.getWeight());
	}
}
