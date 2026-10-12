package et.medco.etrade.contribution;

/** The requested licence is not on that TIN's registration. */
public class LicenceNotFoundException extends RuntimeException {

    public LicenceNotFoundException(String tin, String licenceId) {
        super("Registration " + tin + " holds no licence with id '" + licenceId + "'");
    }
}
