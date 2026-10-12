package et.medco.etrade.contribution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A contribution payment covering one or more trade licences.
 *
 * <p>Several licences settle under a single reference rather than one payment each: the
 * payer selects what they are paying for, and gets back one receipt to quote.
 *
 * <p>Settlement here is <strong>simulated</strong>: nothing is sent to Telebirr, CBE or any
 * other provider, and confirming a payment only moves this record's status. It exists so the
 * demo can show the whole flow end to end; wiring a real provider means replacing
 * {@code PaymentService#confirm} with that provider's callback.
 */
public record Payment(
        String reference,
        String tin,
        String businessName,
        List<PaymentLine> lines,
        BigDecimal total,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        Instant createdAt,
        Instant settledAt) {

    public Payment settled(Instant when) {
        return new Payment(reference, tin, businessName, lines, total, currency, method,
                PaymentStatus.PAID, createdAt, when);
    }
}
