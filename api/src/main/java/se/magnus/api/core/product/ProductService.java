package se.magnus.api.core.product;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

public interface ProductService {

    @GetMapping(value = "/products/{productId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
     Product getProduct(@PathVariable int productId);

    @PostMapping(value="/products", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Product createProduct(@RequestBody Product product);
    
    @DeleteMapping("/products/{productId}")
    public void deleteProduct(@PathVariable("productId")int productId);
    
}
