package se.magnus.api.core.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Product {
    private int productId;
    private String name;
    private int weight;
    private String serviceAddress;
	public int getProductId() {
		return productId;
	}    
}
