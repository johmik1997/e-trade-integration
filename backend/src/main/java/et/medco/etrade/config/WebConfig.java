package et.medco.etrade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private final EtradeProperties properties;

    WebConfig(EtradeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // POST is required, and not only for cross-origin callers: browsers attach an Origin
        // header to same-origin POST requests too, so a GET-only mapping makes Spring reject
        // the payment calls through the dev proxy with "Invalid CORS request".
        registry.addMapping("/api/**")
                .allowedOrigins(properties.corsAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type", "Accept")
                .maxAge(3600);
    }
}
