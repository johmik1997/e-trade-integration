package et.medco.etrade.domain;

/**
 * A manager or owner. The scanned photo lives behind {@code photoUrl} instead of being
 * inlined, which keeps a registration response in the low kilobytes.
 */
public record Associate(
        String position,
        LocalizedText name,
        String mobilePhone,
        String regularPhone,
        String photoUrl) {
}
