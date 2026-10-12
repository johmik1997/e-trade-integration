package et.medco.etrade.upstream;

/** The portal answered 4xx (other than "not found") — our request was malformed for it. */
public class UpstreamRejectedException extends UpstreamException {

    private final int status;

    public UpstreamRejectedException(int status, String message) {
        super(message, null);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
