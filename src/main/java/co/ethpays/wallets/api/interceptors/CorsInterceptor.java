package co.ethpays.wallets.api.interceptors;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class CorsInterceptor implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Origin", "Content-Type", "X-Auth-Token", "Authorization", "EthpaysAdmin-Token", "EthpaysSystem-Token")
                .maxAge(3600); // Optional: set the maximum age (in seconds) of the preflight OPTIONS request
    }
}