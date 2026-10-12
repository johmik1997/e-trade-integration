package et.medco.etrade.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

import et.medco.etrade.contribution.LicenceNotFoundException;
import et.medco.etrade.contribution.NoLicenceSelectedException;
import et.medco.etrade.contribution.PaymentNotFoundException;
import et.medco.etrade.service.InvalidTinException;
import et.medco.etrade.service.UnsupportedLanguageException;
import et.medco.etrade.upstream.RegistrationNotFoundException;
import et.medco.etrade.upstream.UpstreamRejectedException;
import et.medco.etrade.upstream.UpstreamUnavailableException;

/**
 * Maps our failure modes onto RFC 9457 problem responses, and keeps upstream status codes
 * from leaking straight through: a portal 500 is a 502 for our callers, not a 500 of ours.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String TYPE_BASE = "https://medco.et/problems/";

    @ExceptionHandler(InvalidTinException.class)
    ProblemDetail invalidTin(InvalidTinException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid TIN", e.getMessage(), "invalid-tin");
    }

    @ExceptionHandler(UnsupportedLanguageException.class)
    ProblemDetail unsupportedLanguage(UnsupportedLanguageException e) {
        return problem(HttpStatus.BAD_REQUEST, "Unsupported language", e.getMessage(), "unsupported-language");
    }

    @ExceptionHandler(RegistrationNotFoundException.class)
    ProblemDetail notFound(RegistrationNotFoundException e) {
        ProblemDetail detail = problem(HttpStatus.NOT_FOUND, "Registration not found",
                e.getMessage(), "registration-not-found");
        detail.setProperty("tin", e.tin());
        return detail;
    }

    @ExceptionHandler(NoLicenceSelectedException.class)
    ProblemDetail noLicenceSelected(NoLicenceSelectedException e) {
        return problem(HttpStatus.BAD_REQUEST, "No licence selected", e.getMessage(), "no-licence-selected");
    }

    @ExceptionHandler(LicenceNotFoundException.class)
    ProblemDetail licenceNotFound(LicenceNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Licence not found", e.getMessage(), "licence-not-found");
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ProblemDetail paymentNotFound(PaymentNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Payment not found", e.getMessage(), "payment-not-found");
    }

    @ExceptionHandler(UpstreamRejectedException.class)
    ProblemDetail rejected(UpstreamRejectedException e) {
        log.warn("e-Trade rejected a lookup: {}", e.getMessage());
        ProblemDetail detail = problem(HttpStatus.BAD_GATEWAY, "e-Trade rejected the request",
                e.getMessage(), "upstream-rejected");
        detail.setProperty("upstreamStatus", e.status());
        return detail;
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    ProblemDetail unavailable(UpstreamUnavailableException e) {
        log.error("e-Trade unavailable: {}", e.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "e-Trade is unavailable",
                e.getMessage(), "upstream-unavailable");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(TYPE_BASE + type));
        return problem;
    }
}
