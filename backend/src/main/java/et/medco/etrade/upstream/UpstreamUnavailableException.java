package et.medco.etrade.upstream;

/** The portal timed out, refused the connection, or answered 5xx on every attempt. */
public class UpstreamUnavailableException extends UpstreamException {

    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
