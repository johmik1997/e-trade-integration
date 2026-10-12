package et.medco.etrade.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the upstream e-Trade registration API and for our own exposure of it.
 *
 * @param baseUrl            root of the upstream API, e.g. {@code https://etrade.gov.et}
 * @param connectTimeout     TCP connect timeout for upstream calls
 * @param readTimeout        response timeout for upstream calls; the portal is slow, so this is generous
 * @param retries            extra attempts after the first one, for transport errors and 5xx only
 * @param referer            sent as the {@code Referer} header on every upstream call. The portal's
 *                           WAF answers {@code 417 Invalid referer headerr} (their typo) unless this
 *                           is on the portal's own origin; the path within it does not matter.
 * @param supportedLanguages language codes we are willing to pass through to the upstream path.
 *                           Only {@code en} by default: {@code am} and {@code or} return dates in
 *                           the Ethiopian calendar ({@code 19/7/2011} for the same registration
 *                           {@code en} reports as {@code 3/28/2019}), which this service does not
 *                           yet convert, so it would surface them as unparseable.
 * @param legalConditions    code -> human label for the {@code LegalCondtion} field; unmapped codes
 *                           surface with a null label rather than a guessed one
 * @param photoCache         bounds for the content-addressed associate photo store
 * @param registrationCache  bounds for cached lookups
 * @param corsAllowedOrigins origins allowed to call this service from a browser
 * @param sslBundle          name of a {@code spring.ssl.bundle.*} entry whose trust store is used
 *                           for the upstream connection. Needed because the portal serves an
 *                           incomplete certificate chain, which the JDK will not complete on its
 *                           own. Null leaves the JDK default trust store in place.
 */
@ConfigurationProperties(prefix = "etrade")
public record EtradeProperties(
        @DefaultValue("https://etrade.gov.et") String baseUrl,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout,
        @DefaultValue("1") int retries,
        @DefaultValue("https://etrade.gov.et/") String referer,
        @DefaultValue({"en"}) List<String> supportedLanguages,
        Map<String, String> legalConditions,
        @DefaultValue PhotoCache photoCache,
        @DefaultValue RegistrationCache registrationCache,
        @DefaultValue({"http://localhost:5173"}) List<String> corsAllowedOrigins,
        String sslBundle) {

    public record PhotoCache(
            @DefaultValue("64MB") org.springframework.util.unit.DataSize maxSize,
            @DefaultValue("6h") Duration ttl) {
    }

    public record RegistrationCache(
            @DefaultValue("2000") long maxEntries,
            @DefaultValue("1h") Duration ttl) {
    }

    public Map<String, String> legalConditions() {
        return legalConditions == null ? Map.of() : legalConditions;
    }
}
