package et.medco.etrade.service;

/** The caller's TIN is not shaped like an Ethiopian TIN, so we never bother the portal. */
public class InvalidTinException extends RuntimeException {

    public InvalidTinException(String message) {
        super(message);
    }
}
