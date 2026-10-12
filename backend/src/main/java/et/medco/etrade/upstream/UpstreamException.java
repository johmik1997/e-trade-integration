package et.medco.etrade.upstream;

/** Base type for every failure that originates at the e-Trade portal. */
public abstract class UpstreamException extends RuntimeException {

    protected UpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
