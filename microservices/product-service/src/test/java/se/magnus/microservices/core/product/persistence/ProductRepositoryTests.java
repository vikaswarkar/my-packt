package se.magnus.microservices.core.product.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(SpringExtension.class)
@DataMongoTest
class ProductRepositoryTests {
	
	@Autowired
	ProductRepository repository;
	
	private ProductEntity savedEntity;
	
	@BeforeEach
	public void setUp() {
		System.out.println("Cleaning upp....................");
	}
	
	@BeforeEach
	public void setupDb() {
		System.out.println("Setting up db");
		repository.deleteAll();
		ProductEntity entity = new ProductEntity(1, "n1", 1);
		this.savedEntity = repository.save(entity);
		assertEqualsProduct(entity, savedEntity);
	}
	
	@AfterEach
	public void tearDown() {
		System.out.println("---->>>>>>>>Tearing down the test setup");
	}
	
	@Test
	void create() {
		ProductEntity newEntity = new ProductEntity(2, "n2", 2);
		repository.save(newEntity);
		
		ProductEntity foundEntity = repository.findById(newEntity.getId()).get();
		assertEqualsProduct(newEntity, foundEntity);
		
		assertEquals(2, repository.count());
	}

	@Test
	public void update() {
		savedEntity.setName("n2");
		repository.save(savedEntity);
		
		ProductEntity foundEntity = repository.findById(savedEntity.getId()).get();
		
		assertEquals(1, foundEntity.getVersion());
		assertEquals("n2",foundEntity.getName());
	}
	
	@Test
	public void delete() {
		repository.delete(savedEntity);
		assertFalse(repository.existsById(savedEntity.getId()));
	}
	
	@Test
	public void getByProductId(){
		ProductEntity foundEntity = repository.findByProductId(1).get();
		assertEqualsProduct(savedEntity, foundEntity);
	}
	
	@Test
	public void duplicateError() {
		ProductEntity duplicateProduct = new ProductEntity(savedEntity.getProductId(), "n1", 1);
		Assertions.assertThrows(DuplicateKeyException.class, ()-> repository.save(duplicateProduct) );
	}
	
	@Test
	public void optimisticLockError() {
		assertEquals(0, savedEntity.getVersion());
		ProductEntity entity1 = repository.findById(savedEntity.getId()).get();
		ProductEntity entity2 = repository.findById(savedEntity.getId()).get();
		
		entity1.setName("n1-1");
		repository.save(entity1);
		
		assertEquals(1, repository.findById(savedEntity.getId()).get().getVersion());
		
		try {
			entity2.setName("n2-2");
			repository.save(entity2);
		}catch(OptimisticLockingFailureException ex) {
			
		}
		
		ProductEntity foundEntity = repository.findById(savedEntity.getId()).get();
		assertEquals(1, foundEntity.getVersion());
		assertEquals("n1-1", foundEntity.getName());
		
	}
	
	private void assertEqualsProduct(ProductEntity expected, ProductEntity actual) {
		assertEquals(expected.getId(), actual.getId());
		assertEquals(expected.getVersion(), actual.getVersion());
		assertEquals(expected.getProductId(), actual.getProductId());
		assertEquals(expected.getName(), actual.getName());
		assertEquals(expected.getWeight(), actual.getWeight());
	}
}
