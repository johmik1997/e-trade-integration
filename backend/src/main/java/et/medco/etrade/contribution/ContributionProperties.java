package et.medco.etrade.contribution;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The contribution fee schedule, keyed by the portal's {@code LegalCondtion} code.
 *
 * <p>The amounts that ship here are <strong>demo placeholders</strong>, not the real
 * published tariff: e-Trade documents no code list, so the categories themselves are
 * inferred from sampled records ({@code 1} appears on sole proprietors, {@code 2} on
 * PLCs). Replace them with the official schedule before this is used for anything real.
 *
 * @param currency  ISO currency code the fees are expressed in
 * @param categories legal-condition code -> category
 * @param fallback  used when a record's legal condition is not in {@code categories}, so a
 *                  lookup still produces a quote instead of a dead end
 */
@ConfigurationProperties(prefix = "contribution")
public record ContributionProperties(
        @DefaultValue("ETB") String currency,
        Map<String, Category> categories,
        @DefaultValue Fallback fallback) {

    public record Category(String label, BigDecimal fee) {
    }

    public record Fallback(
            @DefaultValue("Standard contribution") String label,
            @DefaultValue("2500.00") BigDecimal fee) {
    }

    public Map<String, Category> categories() {
        return categories == null ? Map.of() : categories;
    }
}
