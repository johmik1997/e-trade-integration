package et.medco.etrade.upstream;

import java.io.IOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import et.medco.etrade.config.EtradeProperties;

/**
 * Thin, blocking adapter over the e-Trade portal. Its only job is to turn transport and
 * status noise into the four outcomes the rest of the app understands: a body, not-found,
 * rejected, unavailable.
 */
@Component
public class EtradeClient {

    private static final Logger log = LoggerFactory.getLogger(EtradeClient.class);
    private static final String PATH = "/api/Registration/GetRegistrationInfoByTin/{tin}/{lang}";

    private final RestClient restClient;
    private final int attempts;

    public EtradeClient(RestClient etradeRestClient, EtradeProperties properties) {
        this.restClient = etradeRestClient;
        this.attempts = Math.max(1, properties.retries() + 1);
    }

    public UpstreamRegistration fetchByTin(String tin, String language) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return requestOnce(tin, language);
            }
            catch (UpstreamUnavailableException e) {
                last = e;
                log.warn("e-Trade lookup for TIN {} failed on attempt {}/{}: {}",
                        tin, attempt, attempts, e.getMessage());
                if (attempt < attempts) {
                    sleepBeforeRetry(attempt);
                }
            }
        }
        throw last;
    }

    private UpstreamRegistration requestOnce(String tin, String language) {
        UpstreamRegistration body;
        try {
            body = restClient.get()
                    .uri(PATH, tin, language)
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, (request, response) -> {
                        throw new RegistrationNotFoundException(tin);
                    })
                    .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                        throw new UpstreamRejectedException(response.getStatusCode().value(),
                                "e-Trade rejected the lookup with HTTP " + response.getStatusCode().value());
                    })
                    .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                        throw new UpstreamUnavailableException(
                                "e-Trade answered HTTP " + response.getStatusCode().value(), null);
                    })
                    .body(UpstreamRegistration.class);
        }
        catch (ResourceAccessException e) {
            throw new UpstreamUnavailableException("e-Trade is unreachable: " + rootMessage(e), e);
        }
        if (body == null || body.isEmpty()) {
            throw new RegistrationNotFoundException(tin);
        }
        return body;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(Duration.ofMillis(250L * attempt));
        }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new UpstreamUnavailableException("Interrupted while retrying e-Trade", interrupted);
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause instanceof IOException io ? io.toString() : cause.getMessage();
    }
}
