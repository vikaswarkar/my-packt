package se.magnus.microservices.core.product.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.AsyncProductService;
import se.magnus.api.core.product.Product;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ReactiveProductRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

import static reactor.core.publisher.Mono.error;

@RestController
public class AsyncProductServiceImpl implements AsyncProductService {

	@Autowired
	private ReactiveProductRepository repository;

	@Autowired
	private ProductMapper mapper;

	@Autowired
	private ServiceUtil serviceUtil;

//	public AsyncProductServiceImpl(ReactiveProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
//        this.repository = repository;
//        this.mapper = mapper;
//        this.serviceUtil = serviceUtil;
//	}
	
	@Override
	public Mono<Product> getProduct(int productId) {
		if (productId < 0) throw new InvalidInputException("Invalid Product Id: " + productId);
		
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
	public Product createProduct(Product product) {
		if (product.getProductId() < -1) throw new InvalidInputException("Invalid Product Id: " + product.getProductId());
		
		ProductEntity entity = mapper.apiToEntity(product);
		
		return repository.save(entity)
				.log()
				.onErrorMap(DuplicateKeyException.class, 
						ex -> new InvalidInputException("Duplicate Key, ProductId: " + product.getProductId()))
				.map( e -> mapper.entityToApi(e)).log().block();
	}

	@Override
	public void deleteProduct(int productId) {
		if (productId < -1) throw new InvalidInputException("Invalid Product Id: " + productId);
		
		repository.findByProductId(productId).log().map(e -> repository.delete(e)).flatMap(e -> e).block();
		
	}

}
