package et.medco.etrade.upstream;

/** No registration exists upstream for the requested TIN. */
public class RegistrationNotFoundException extends UpstreamException {

    private final String tin;

    public RegistrationNotFoundException(String tin) {
        super("No registration found for TIN " + tin, null);
        this.tin = tin;
    }

    public String tin() {
        return tin;
    }
}
