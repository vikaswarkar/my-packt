package se.magnus.api.core.product;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/reactive")
public interface ReactiveProductService {

    /**
     * Sample usage: curl $HOST:$PORT/product/1
     *
     * @param productId
     * @return the product, if found, else null
     */
    @GetMapping(
        value    = "/product/{productId}",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
     Mono<Product> getProduct(@PathVariable int productId);
    
    
    @PostMapping(value = "/product", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<Product> createProduct(@RequestBody Product product);
    
    @DeleteMapping(value = "/product/{productId}")
    public void deleteProduct(@PathVariable("productId")int productId);
    
}
