package et.medco.etrade.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import et.medco.etrade.domain.PortalDate;

/**
 * Parses the portal's date strings.
 *
 * <p>In English the portal formats dates US-style, e.g. {@code 3/28/2019}, and they are
 * Gregorian, not Ethiopian: the sample registration {@code BL/AA/2/0018364/2011} (Ethiopian
 * year 2011) carries {@code RegDate} 3/28/2019, and EC 2011 does span March 2019. Asking the
 * portal for {@code am} or {@code or} instead returns the same dates in the Ethiopian calendar
 * ({@code 19/7/2011}), which is why those languages are not enabled by default.
 */
final class UpstreamDates {

    private static final Logger log = LoggerFactory.getLogger(UpstreamDates.class);

    private static final DateTimeFormatter US_STYLE =
            DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /**
     * Placeholders the portal writes into date fields, seen on licences that have never been
     * renewed. These are expected, so they are not logged as surprises.
     */
    private static final Set<String> PLACEHOLDERS = Set.of("data not available", "n/a", "-", "--");

    private UpstreamDates() {
    }

    /** @return null when the portal sent nothing at all, otherwise a date and/or its raw text */
    static PortalDate parse(String raw) {
        String trimmed = raw == null || raw.isBlank() ? null : raw.strip();
        if (trimmed == null) {
            return null;
        }
        if (PLACEHOLDERS.contains(trimmed.toLowerCase(java.util.Locale.ROOT))) {
            return new PortalDate(null, trimmed);
        }
        try {
            return new PortalDate(LocalDate.parse(trimmed, US_STYLE), trimmed);
        }
        catch (DateTimeParseException e) {
            log.warn("Could not parse upstream date '{}'; exposing it as raw text only", trimmed);
            return new PortalDate(null, trimmed);
        }
    }
}
