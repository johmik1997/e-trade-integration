package et.medco.etrade;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.util.unit.DataSize;

import et.medco.etrade.config.EtradeProperties;

/** Small builder so tests are not rewritten every time a property is added. */
public final class TestProperties {

    private TestProperties() {
    }

    public static EtradeProperties properties() {
        return properties(1, Map.of());
    }

    public static EtradeProperties properties(int retries, Map<String, String> legalConditions) {
        return new EtradeProperties(
                "https://etrade.example",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                retries,
                "https://etrade.example/",
                List.of("en", "am"),
                legalConditions,
                new EtradeProperties.PhotoCache(DataSize.ofMegabytes(8), Duration.ofMinutes(10)),
                new EtradeProperties.RegistrationCache(100, Duration.ofMinutes(10)),
                List.of("http://localhost:5173"),
                null);
    }
}
