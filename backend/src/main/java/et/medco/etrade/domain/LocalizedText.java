package et.medco.etrade.domain;

/**
 * A value the portal carries in two scripts. Either side can be absent, so consumers
 * should fall back rather than assume both are present.
 */
public record LocalizedText(String en, String am) {

    public static LocalizedText of(String en, String am) {
        return new LocalizedText(blankToNull(en), blankToNull(am));
    }

    /** Whichever script is available, English first. Null when the portal gave us neither. */
    public String preferred() {
        return en != null ? en : am;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
