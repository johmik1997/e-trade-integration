package et.medco.etrade.contribution;

import java.math.BigDecimal;

/** One licence's contribution within a payment. */
public record PaymentLine(
        String licenceId,
        String licenceNumber,
        String categoryLabel,
        BigDecimal amount) {
}
