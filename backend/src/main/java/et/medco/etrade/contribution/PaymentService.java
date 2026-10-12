package et.medco.etrade.contribution;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Records contribution payments.
 *
 * <p>Storage is an in-process map and settlement is simulated — see {@link Payment}. For
 * anything beyond the demo this wants a database and a real provider callback; the service
 * boundary is shaped so that swap does not reach the controller or the UI.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";   // no I/O/0/1

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /**
     * @param quotes one per selected licence, already priced; must be non-empty and all for
     *               the same taxpayer
     */
    public Payment initiate(List<FeeQuote> quotes, PaymentMethod method) {
        if (quotes.isEmpty()) {
            throw new NoLicenceSelectedException();
        }
        var lines = quotes.stream()
                .map(quote -> new PaymentLine(quote.licenceId(), quote.licenceNumber(),
                        quote.category().label(), quote.category().fee()))
                .toList();
        var total = lines.stream()
                .map(PaymentLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var first = quotes.getFirst();
        var now = Instant.now();
        var payment = new Payment(
                nextReference(now),
                first.tin(),
                first.businessName(),
                lines,
                total,
                first.category().currency(),
                method,
                PaymentStatus.PENDING,
                now,
                null);
        payments.put(payment.reference(), payment);
        log.info("Contribution payment {} initiated for TIN {}: {} licence(s), {} {}",
                payment.reference(), payment.tin(), lines.size(), total, payment.currency());
        return payment;
    }

    /**
     * Marks a payment settled. Idempotent: confirming an already-paid reference returns it
     * unchanged rather than re-stamping it, so a double-tap in the UI cannot rewrite history.
     */
    public Payment confirm(String reference) {
        Payment existing = payments.get(reference);
        if (existing == null) {
            throw new PaymentNotFoundException(reference);
        }
        if (existing.status() == PaymentStatus.PAID) {
            return existing;
        }
        Payment settled = existing.settled(Instant.now());
        payments.put(reference, settled);
        log.info("Contribution payment {} settled (simulated)", reference);
        return settled;
    }

    public Payment require(String reference) {
        return find(reference).orElseThrow(() -> new PaymentNotFoundException(reference));
    }

    public Optional<Payment> find(String reference) {
        return Optional.ofNullable(payments.get(reference));
    }

    /** Payments recorded for a TIN, newest first — the demo's receipt history. */
    public List<Payment> forTin(String tin) {
        return payments.values().stream()
                .filter(payment -> payment.tin().equals(tin))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }

    private String nextReference(Instant when) {
        var suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return "CNT-" + DAY.format(when) + "-" + suffix;
    }
}
