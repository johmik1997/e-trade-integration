package et.medco.etrade.contribution;

import java.math.BigDecimal;

/**
 * The fee band a registration falls into.
 *
 * @param legalConditionCode the portal's code this was derived from
 * @param inferred           true when the code was not in the configured schedule and the
 *                           fallback band was applied, so callers can say so rather than
 *                           presenting a guessed fee as authoritative
 */
public record ContributionCategory(
        String legalConditionCode,
        String label,
        BigDecimal fee,
        String currency,
        boolean inferred) {
}
