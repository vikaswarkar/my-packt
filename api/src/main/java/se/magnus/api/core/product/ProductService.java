package se.magnus.api.core.product;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public interface ProductService {

    @GetMapping(value = "/products/{productId}", produces = APPLICATION_JSON_VALUE)
     Product getProduct(@PathVariable int productId);

    @PostMapping(value="/products", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public Product createProduct(@RequestBody Product product);
    
    @DeleteMapping("/products/{productId}")
    public void deleteProduct(@PathVariable("productId")int productId);
    
}
