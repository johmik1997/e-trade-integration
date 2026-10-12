package et.medco.etrade.config;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** The portal's incomplete certificate chain is configured around, not coded around. */
class RestClientConfigTest {

    @EnableConfigurationProperties(EtradeProperties.class)
    static class Enable {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class))
            .withUserConfiguration(Enable.class, RestClientConfig.class);

    @Test
    void buildsTheClientOnTheJdkDefaultTrustStoreWhenNoBundleIsNamed() {
        runner.run(context -> assertThat(context).hasSingleBean(RestClient.class));
    }

    @Test
    void buildsTheClientOnAConfiguredTrustStoreBundle(@TempDir Path dir) throws Exception {
        Path truststore = emptyPkcs12(dir.resolve("etrade-truststore.p12"));

        runner.withPropertyValues(
                "spring.ssl.bundle.jks.etrade.truststore.location=file:" + truststore,
                "spring.ssl.bundle.jks.etrade.truststore.password=changeit",
                "spring.ssl.bundle.jks.etrade.truststore.type=PKCS12",
                "etrade.ssl-bundle=etrade")
                .run(context -> assertThat(context).hasSingleBean(RestClient.class));
    }

    @Test
    void failsAtStartupWithTheBundleNameWhenItDoesNotExist() {
        runner.withPropertyValues("etrade.ssl-bundle=typo")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("typo"));
    }

    private static Path emptyPkcs12(Path path) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        try (OutputStream out = Files.newOutputStream(path)) {
            store.store(out, "changeit".toCharArray());
        }
        return path;
    }
}
