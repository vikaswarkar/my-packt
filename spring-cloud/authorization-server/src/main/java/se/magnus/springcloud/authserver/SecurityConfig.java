package se.magnus.springcloud.authserver;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

/**
 * For configuring the end users recognized by this Authorization Server
 */
//@Configuration
class SecurityConfig extends WebSecurityConfigurerAdapter {

//	@Override
//	protected void configure(HttpSecurity http) throws Exception {
//		http
//			.authorizeRequests()
//				.antMatchers("/actuator/**").permitAll()
//				.mvcMatchers("/.well-known/jwks.json").permitAll()
//				.anyRequest().authenticated()
//				.and()
//			.httpBasic()
//				.and()
//			.csrf().ignoringRequestMatchers(request -> "/introspect".equals(request.getRequestURI()));
//	}

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .anyRequest()
                .authenticated()
                .and()
                .httpBasic();
    }

    @Override
    public void configure(AuthenticationManagerBuilder amb) throws Exception {
        amb.inMemoryAuthentication().passwordEncoder(NoOpPasswordEncoder.getInstance())
                .withUser("magnus").password("password")
                .authorities("USER");
    }

//	@Bean
//	@Override
//	public UserDetailsService userDetailsService() {
//		return new InMemoryUserDetailsManager(
//				User.withDefaultPasswordEncoder()
//					.username("magnus")
//					.password("password")
//					.roles("USER")
//					.build());
//	}
}
