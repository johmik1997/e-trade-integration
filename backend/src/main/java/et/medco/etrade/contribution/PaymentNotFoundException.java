package et.medco.etrade.contribution;

/** No payment exists with that reference. */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String reference) {
        super("No payment with reference " + reference);
    }
}
