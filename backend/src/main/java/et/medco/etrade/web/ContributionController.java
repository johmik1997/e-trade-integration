package et.medco.etrade.web;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import et.medco.etrade.contribution.ContributionService;
import et.medco.etrade.contribution.FeeQuote;
import et.medco.etrade.contribution.Payment;
import et.medco.etrade.contribution.PaymentMethod;
import et.medco.etrade.contribution.PaymentService;

/**
 * The step after a lookup: price the contribution for a licence, then pay it.
 *
 * <p>Payment is simulated; see {@code Payment}.
 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class ContributionController {

    private final ContributionService contributions;
    private final PaymentService payments;

    ContributionController(ContributionService contributions, PaymentService payments) {
        this.contributions = contributions;
        this.payments = payments;
    }

    /** Every licence on the record, priced, so the payer can pick one. */
    @GetMapping("/registrations/{tin}/contributions")
    public List<FeeQuote> quotes(@PathVariable String tin) {
        return contributions.quoteAll(tin);
    }

    @GetMapping("/registrations/{tin}/contributions/{licenceId}")
    public FeeQuote quote(@PathVariable String tin, @PathVariable String licenceId) {
        return contributions.quote(tin, licenceId);
    }

    @PostMapping("/payments")
    public ResponseEntity<Payment> initiate(@RequestBody PaymentRequest request) {
        List<FeeQuote> quotes = contributions.quote(request.tin(), request.licenceIds());
        Payment payment = payments.initiate(quotes, request.method());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    /** Stands in for a provider callback. */
    @PostMapping("/payments/{reference}/confirm")
    public Payment confirm(@PathVariable String reference) {
        return payments.confirm(reference);
    }

    @GetMapping("/payments/{reference}")
    public Payment payment(@PathVariable String reference) {
        return payments.require(reference);
    }

    @GetMapping("/registrations/{tin}/payments")
    public List<Payment> history(@PathVariable String tin) {
        return payments.forTin(tin);
    }

    /**
     * The amount is deliberately not accepted from the client: it is re-derived from the
     * registration's category server-side, so a payer cannot choose their own fee.
     */
    public record PaymentRequest(
            @NotNull String tin,
            @NotEmpty List<String> licenceIds,
            @NotNull PaymentMethod method) {
    }
}
