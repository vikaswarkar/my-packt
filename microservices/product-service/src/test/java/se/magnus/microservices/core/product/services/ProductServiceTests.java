package se.magnus.microservices.core.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import se.magnus.api.core.product.Product;
import se.magnus.microservices.core.product.persistence.ProductRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"spring.data.mongodb.port:0",
        "eureka.client.enabled=false"})

public class ProductServiceTests {

    private final String PRODUCT_BASE_URL = "/products";

    @Autowired
    WebApplicationContext webApplicationContext;

    MockMvc mockMvc;

    @Autowired
    private ProductRepository repository;

    @Test
    public void contextLoads() {
    }

    @BeforeEach
    public void setupDb() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        repository.deleteAll();
    }

    @Test
    public void getProductById() throws Exception {
        int productId = 1;

        postAndVerifyProduct(productId, HttpStatus.OK);

        assertTrue(repository.findByProductId(productId).isPresent());

        getAndVerifyProduct(productId, HttpStatus.OK)
                .andExpect(MockMvcResultMatchers.jsonPath("$.productId").value(productId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Name " + productId));
    }

    @Test
    public void duplicateError() throws Exception {
        int productId = 1;
        postAndVerifyProduct(productId, HttpStatus.OK);
        assertTrue(repository.findByProductId(productId).isPresent());
        postAndVerifyProduct(productId, UNPROCESSABLE_ENTITY)
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(PRODUCT_BASE_URL))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Duplicate Key, Product Id " + productId));
    }

    @Test
    public void deleteProduct() throws Exception {
        int productId = 1;
        postAndVerifyProduct(1, HttpStatus.OK);
        assertTrue(repository.findByProductId(productId).isPresent());
        deleteAndVerifyProduct(productId, HttpStatus.OK);
        assertFalse(repository.findByProductId(productId).isPresent());
        deleteAndVerifyProduct(productId, HttpStatus.OK);
    }

    @Test
    public void getProductInvalidParameterString() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(PRODUCT_BASE_URL + "/no-integer")
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is(BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(PRODUCT_BASE_URL + "/no-integer"));
//                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Type mismatch."));

    }

    @Test
    public void getProductNotFound() throws Exception {
        int productIdNotFound = 13;
        mockMvc.perform(MockMvcRequestBuilders.get(PRODUCT_BASE_URL + "/" + productIdNotFound)
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(PRODUCT_BASE_URL + "/" + productIdNotFound))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("No Product found for productId " + productIdNotFound));
    }

    @Test
    public void getProductInvalidParameterNegativeValue() throws Exception {
        int productIdInvalid = -1;
        mockMvc.perform(MockMvcRequestBuilders.get(PRODUCT_BASE_URL + "/" + productIdInvalid)
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is(UNPROCESSABLE_ENTITY.value()))
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.path").value(PRODUCT_BASE_URL + "/" + productIdInvalid))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid productId: " + productIdInvalid));
    }

    private ResultActions postAndVerifyProduct(int productId, HttpStatus expectedStatus) throws Exception {
        Product product = new Product(productId, "Name " + productId, productId, "SA");
        return mockMvc.perform(MockMvcRequestBuilders.post(PRODUCT_BASE_URL)
                        .content(jsonAsString(product))
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus.value()))
                .andExpect(MockMvcResultMatchers.header().string("Content-Type", APPLICATION_JSON_VALUE));
    }

    private ResultActions getAndVerifyProduct(int productId, HttpStatus expectedStatus) throws Exception {
        return getAndVerifyProduct("/" + productId, expectedStatus);
    }

    private ResultActions getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(PRODUCT_BASE_URL + productIdPath)
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus.value()))
                .andExpect(MockMvcResultMatchers.content().contentType(APPLICATION_JSON_VALUE));
    }

    private ResultActions deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(PRODUCT_BASE_URL + "/" + productId)
                        .accept(APPLICATION_JSON_VALUE)
                        .contentType(APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus.value()));
    }


    private String jsonAsString(Product product) throws Exception {
        ObjectMapper om = new ObjectMapper();
        return om.writeValueAsString(product);
    }
}

