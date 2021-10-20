package se.magnus.microservices.composite.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.DefaultSecurityFilterChain;

//@Configuration
//@EnableWebSecurity
//@Order(value = 1)
public class SecurityConfigHHHH //extends WebSecurityConfigurerAdapter
{

////    @Bean
//    DefaultSecurityFilterChain springSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
//        httpSecurity.authorizeRequests()
//                .antMatchers("/actuators/**").permitAll()
//                .antMatchers(HttpMethod.POST, "/product-composite/**").hasAnyAuthority("SCOPE_product:write")
//                .antMatchers(HttpMethod.DELETE, "/product-composite/**").hasAnyAuthority("SCOPE_product:write")
//                .antMatchers(HttpMethod.GET,"/product-composite/**").hasAnyAuthority("SCOPE_product:read")
//                .anyRequest().authenticated()
////                .anyExchange().authenticated()
//                .and()
//                .oauth2ResourceServer()
//                .jwt();
//        return httpSecurity.build();
//    }
}
