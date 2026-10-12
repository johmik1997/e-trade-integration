package et.medco.etrade.contribution;

import java.time.Instant;

/**
 * A priced contribution for one trade licence, ready to be paid.
 *
 * @param licenceId     the portal's licence id this quote is for
 * @param licenceNumber shown to the payer so they can confirm what they are paying for
 */
public record FeeQuote(
        String tin,
        String businessName,
        String licenceId,
        String licenceNumber,
        ContributionCategory category,
        Instant quotedAt) {
}
