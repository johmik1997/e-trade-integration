package et.medco.etrade.domain;

import java.time.LocalDate;

/**
 * A date as the portal gave it, plus our interpretation of it.
 *
 * <p>Both halves are kept because the portal's date fields are not reliably dates: a licence
 * that has never been renewed carries the literal string {@code "Data not available"}. Callers
 * that want a date use {@code iso} and handle null; callers that want to show exactly what the
 * registry says use {@code raw}.
 */
public record PortalDate(LocalDate iso, String raw) {

    /** True when the portal gave us text that is not a date, such as "Data not available". */
    public boolean isUnparsed() {
        return iso == null && raw != null;
    }
}
