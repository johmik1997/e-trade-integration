package et.medco.etrade.contribution;

/** A payment must cover at least one licence. */
public class NoLicenceSelectedException extends RuntimeException {

    public NoLicenceSelectedException() {
        super("Select at least one trade licence to pay a contribution for");
    }
}
