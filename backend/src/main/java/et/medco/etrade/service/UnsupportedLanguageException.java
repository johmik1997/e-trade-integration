package et.medco.etrade.service;

import java.util.List;

/** The requested language is not one we pass through to the portal. */
public class UnsupportedLanguageException extends RuntimeException {

    public UnsupportedLanguageException(String language, List<String> supported) {
        super("Unsupported language '" + language + "'; supported: " + String.join(", ", supported));
    }
}
