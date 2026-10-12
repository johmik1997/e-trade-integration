package et.medco.etrade.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    /**
     * Dedicated builder for the upstream portal. Kept separate from the application-wide
     * {@code RestClient.Builder} so its timeouts and trust settings cannot leak into other
     * callers.
     */
    @Bean
    RestClient etradeRestClient(EtradeProperties properties, SslBundles sslBundles) {
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());

        if (properties.sslBundle() != null && !properties.sslBundle().isBlank()) {
            settings = settings.withSslBundle(trustBundle(properties.sslBundle(), sslBundles));
        }

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                // Without this the portal rejects every call with 417; see EtradeProperties#referer.
                .defaultHeader("Referer", properties.referer())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }

    /**
     * A misnamed bundle would otherwise surface much later as an opaque TLS handshake
     * failure, so fail at startup with the name that was actually asked for.
     */
    private static org.springframework.boot.ssl.SslBundle trustBundle(String name, SslBundles sslBundles) {
        try {
            return sslBundles.getBundle(name);
        }
        catch (NoSuchSslBundleException e) {
            throw new IllegalStateException("etrade.ssl-bundle is set to '" + name
                    + "', but no such bundle is configured under spring.ssl.bundle.*", e);
        }
    }
}
