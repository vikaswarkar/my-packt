package se.magnus.microservices.composite.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;

@SpringBootApplication
@EnableSwagger2
@ComponentScan("se.magnus")
public class ProductCompositeServiceApplication {

	@LoadBalanced
	@Bean
	RestTemplate restTemplate() {

		return new RestTemplate();
		}


	@Value("${api.common.version}")           String apiVersion;
	@Value("${api.common.title}")             String apiTitle;
	@Value("${api.common.description}")       String apiDescription;
	@Value("${api.common.termsOfServiceUrl}") String apiTermsOfServiceUrl;
	@Value("${api.common.license}")           String apiLicense;
	@Value("${api.common.licenseUrl}")        String apiLicenseUrl;
	@Value("${api.common.contact.name}")      String apiContactName;
	@Value("${api.common.contact.url}")       String apiContactUrl;
	@Value("${api.common.contact.email}")     String apiContactEmail;

	/**
	 * Will exposed on $HOST:$PORT/swagger-ui.html
	 *
	 * @return
	 */
//	@Bean
	public Docket apiDocumentation() {

		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(basePackage("se.magnus.microservices.composite.product"))
				.paths(PathSelectors.any())
				.build()
				.globalResponseMessage(RequestMethod.POST, Collections.emptyList())
				.globalResponseMessage(RequestMethod.GET, Collections.emptyList())
				.globalResponseMessage(RequestMethod.DELETE, Collections.emptyList())
				.apiInfo(new ApiInfo(
						apiTitle,
						apiDescription,
						apiVersion,
						apiTermsOfServiceUrl,
						new Contact(apiContactName, apiContactUrl, apiContactEmail),
						apiLicense,
						apiLicenseUrl,
						Collections.emptyList()
				));
	}

	@Autowired
	ProductCompositeIntegration integration;

	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);

	}

}
