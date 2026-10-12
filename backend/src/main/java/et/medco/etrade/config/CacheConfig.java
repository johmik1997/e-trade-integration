package et.medco.etrade.config;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CacheConfig {

    public static final String REGISTRATIONS = "registrations";

    @Bean
    CacheManager cacheManager(EtradeProperties properties) {
        var bounds = properties.registrationCache();
        var manager = new CaffeineCacheManager(REGISTRATIONS);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(bounds.maxEntries())
                .expireAfterWrite(bounds.ttl().toMillis(), TimeUnit.MILLISECONDS)
                .recordStats());   // so cache.gets/puts/evictions show up under /actuator/metrics
        return manager;
    }
}
