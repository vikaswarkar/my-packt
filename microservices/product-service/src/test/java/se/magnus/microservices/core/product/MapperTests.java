package se.magnus.microservices.core.product;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import se.magnus.api.core.product.Product;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.services.ProductMapper;

import static org.junit.jupiter.api.Assertions.*;

public class MapperTests {

	private ProductMapper mapper = Mappers.getMapper(ProductMapper.class);
	
	@Test
	public void mapperTests() {
		
		assertNotNull(mapper);
		
		Product api = new Product(1, "n", 1, "sa");
		
		ProductEntity entity = mapper.apiToEntity(api);
		
		assertEquals(api.getProductId(), entity.getProductId());
		assertEquals(api.getName(), entity.getName());
		assertEquals(api.getWeight(), entity.getWeight());
		
		entity.setVersion(5);
		Product api2 = mapper.entityToApi(entity);
		
		assertEquals(api2.getProductId(), entity.getProductId());
		assertEquals(api2.getWeight(), entity.getWeight());
		assertEquals(api2.getName(), entity.getName());
		assertNull(api2.getServiceAddress());
		
	}
	
}
