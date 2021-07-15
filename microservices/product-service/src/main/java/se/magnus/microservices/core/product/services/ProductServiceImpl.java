package se.magnus.microservices.core.product.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

@RestController
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;
    
    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil, ProductRepository repository, ProductMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Product getProduct(int productId) {
        log.debug("product return the found product for productId={}", productId);

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        ProductEntity entity = repository.findByProductId(productId)
        		.orElseThrow( () ->  new NotFoundException("No Product found for productId " + productId));

        Product product = mapper.entityToApi(entity);
        product.setServiceAddress(serviceUtil.getServiceAddress());
        return product;
    }

	@Override
	public Product createProduct(Product product) {
		try {
			log.info("Product api {}", product );
			ProductEntity entity = mapper.apiToEntity(product);
			repository.save(entity);
			log.info("Product Entity {}", entity );			
			product = mapper.entityToApi(entity);
			return product;
		}catch(Exception mre) {
			throw new InvalidInputException("Duplicate Key, Product Id " + product.getProductId());
		}
		
	}

	@Override
	public void deleteProduct(int productId) {
		
		repository.findByProductId(productId).ifPresent( e -> {
			repository.delete(e);
		});
	}
}