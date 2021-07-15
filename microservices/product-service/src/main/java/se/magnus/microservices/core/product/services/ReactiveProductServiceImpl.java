package se.magnus.microservices.core.product.services;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ReactiveProductService;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ReactiveProductRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

import static reactor.core.publisher.Mono.error;

@RestController
public class ReactiveProductServiceImpl implements ReactiveProductService {

	private ReactiveProductRepository repository;
	
	private ProductMapper mapper;
	
	private ServiceUtil serviceUtil;
	
	public ReactiveProductServiceImpl(ReactiveProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
	}
	
	@Override
	public Mono<Product> getProduct(int productId) {
		if (productId < -1) throw new InvalidInputException("Invalid Product Id: " + productId);
		
		return repository.findByProductId(productId)
				.switchIfEmpty(error(new NotFoundException("No product found for productId " + productId)))
				.log()
				.map( e -> mapper.entityToApi(e))
				.map(e -> {
					e.setServiceAddress(serviceUtil.getServiceAddress());
					return e;
				});
	}

	@Override
	public Mono<Product> createProduct(Product product) {
		if (product.getProductId() < -1) throw new InvalidInputException("Invalid Product Id: " + product.getProductId());
		
		ProductEntity entity = mapper.apiToEntity(product);
		
		return repository.save(entity)
				.log()
				.onErrorMap(DuplicateKeyException.class, 
						ex -> new InvalidInputException("Duplicate Key, Product Id: " + product.getProductId()))
				.map( e -> mapper.entityToApi(e)).log();
	}

	@Override
	public void deleteProduct(int productId) {
		if (productId < -1) throw new InvalidInputException("Invalid Product Id: " + productId);
		
		repository.findByProductId(productId).log().map(e -> repository.delete(e)).flatMap(e -> e).block();
		
	}

}
