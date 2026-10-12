package et.medco.etrade.service;

import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import et.medco.etrade.config.CacheConfig;
import et.medco.etrade.config.EtradeProperties;
import et.medco.etrade.domain.Associate;
import et.medco.etrade.domain.Registration;
import et.medco.etrade.upstream.EtradeClient;

/**
 * Entry point for TIN lookups: validates, serves from cache when it can, and keeps the
 * cache honest about the photos it references.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private static final Pattern TIN = Pattern.compile("\\d{10}");

    private final EtradeClient client;
    private final RegistrationMapper mapper;
    private final PhotoStore photoStore;
    private final CacheManager cacheManager;
    private final EtradeProperties properties;

    RegistrationService(EtradeClient client, RegistrationMapper mapper, PhotoStore photoStore,
            CacheManager cacheManager, EtradeProperties properties) {
        this.client = client;
        this.mapper = mapper;
        this.photoStore = photoStore;
        this.cacheManager = cacheManager;
        this.properties = properties;
    }

    public Registration lookup(String rawTin, String rawLanguage) {
        String tin = normalizeTin(rawTin);
        String language = normalizeLanguage(rawLanguage);
        String key = tin + '|' + language;
        Cache cache = cacheManager.getCache(CacheConfig.REGISTRATIONS);

        Registration cached = cache == null ? null : cache.get(key, Registration.class);
        if (cached != null) {
            if (photosStillPresent(cached)) {
                return cached;
            }
            // The photo store expires on its own schedule; rather than hand out URLs that
            // 404, drop this entry and fetch again so the images are repopulated.
            log.debug("Cached registration for {} lost its photos; refetching", tin);
            cache.evict(key);
        }

        Registration fresh = mapper.toRegistration(client.fetchByTin(tin, language));
        if (cache != null) {
            cache.put(key, fresh);
        }
        return fresh;
    }

    private boolean photosStillPresent(Registration registration) {
        return registration.associates().stream()
                .map(Associate::photoUrl)
                .filter(url -> url != null)
                .allMatch(url -> photoStore.find(photoIdOf(url)).isPresent());
    }

    private static String photoIdOf(String photoUrl) {
        return photoUrl.substring(photoUrl.lastIndexOf('/') + 1);
    }

    /** TINs are sometimes pasted with spaces or dashes; strip those before validating. */
    private static String normalizeTin(String rawTin) {
        if (rawTin == null) {
            throw new InvalidTinException("A TIN is required");
        }
        String tin = rawTin.replaceAll("[\\s-]", "");
        if (!TIN.matcher(tin).matches()) {
            throw new InvalidTinException("A TIN must be exactly 10 digits, got '" + rawTin + "'");
        }
        return tin;
    }

    private String normalizeLanguage(String rawLanguage) {
        if (rawLanguage == null || rawLanguage.isBlank()) {
            return properties.supportedLanguages().getFirst();
        }
        String language = rawLanguage.strip().toLowerCase(Locale.ROOT);
        if (!properties.supportedLanguages().contains(language)) {
            throw new UnsupportedLanguageException(rawLanguage, properties.supportedLanguages());
        }
        return language;
    }
}
