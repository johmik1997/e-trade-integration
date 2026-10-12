package et.medco.etrade.domain;

/**
 * The upstream {@code LegalCondtion} code, kept verbatim alongside an optional label.
 * The portal publishes no code list, so {@code label} is populated only from the
 * {@code etrade.legal-conditions} mapping in configuration — never guessed.
 */
public record LegalCondition(String code, String label) {
}
