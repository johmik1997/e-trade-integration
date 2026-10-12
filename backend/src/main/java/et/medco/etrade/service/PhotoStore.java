package et.medco.etrade.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import et.medco.etrade.config.EtradeProperties;

/**
 * Holds decoded associate photos out of band so that registration responses stay small.
 *
 * <p>Ids are the SHA-256 of the image bytes, which matters for correctness and not just
 * for tidiness: the registration cache and this store expire independently, so a cached
 * registration can outlive its photos. Because the id is derived from content, re-fetching
 * the registration repopulates the <em>same</em> ids, and previously handed-out URLs start
 * working again instead of breaking permanently.
 */
@Component
public class PhotoStore {

    private static final Logger log = LoggerFactory.getLogger(PhotoStore.class);

    private final Cache<String, StoredPhoto> photos;

    public PhotoStore(EtradeProperties properties) {
        var bounds = properties.photoCache();
        this.photos = Caffeine.newBuilder()
                .maximumWeight(bounds.maxSize().toBytes())
                .weigher((String id, StoredPhoto photo) -> photo.size())
                .expireAfterAccess(bounds.ttl().toMillis(), TimeUnit.MILLISECONDS)
                .recordStats()
                .build();
    }

    /**
     * Decodes a bare upstream base64 payload and stores it.
     *
     * @return the stored photo, or empty when the payload was absent or not decodable
     */
    public Optional<StoredPhoto> store(String base64) {
        if (base64 == null || base64.isBlank()) {
            return Optional.empty();
        }
        byte[] bytes;
        try {
            // getMimeDecoder tolerates the line breaks and stray padding the portal emits.
            bytes = Base64.getMimeDecoder().decode(stripDataUriPrefix(base64));
        }
        catch (IllegalArgumentException e) {
            log.warn("Discarding an associate photo that is not valid base64: {}", e.getMessage());
            return Optional.empty();
        }
        if (bytes.length == 0) {
            return Optional.empty();
        }
        var photo = new StoredPhoto(sha256(bytes), detectContentType(bytes), bytes);
        photos.put(photo.id(), photo);
        return Optional.of(photo);
    }

    public Optional<StoredPhoto> find(String id) {
        return Optional.ofNullable(photos.getIfPresent(id));
    }

    /** The portal sends raw base64, but tolerate a data URI in case that ever changes. */
    private static String stripDataUriPrefix(String value) {
        int comma = value.indexOf(',');
        return value.startsWith("data:") && comma > 0 ? value.substring(comma + 1) : value;
    }

    private static String detectContentType(byte[] bytes) {
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47)) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (startsWith(bytes, 0x47, 0x49, 0x46, 0x38)) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private static boolean startsWith(byte[] bytes, int... magic) {
        if (bytes.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if ((bytes[i] & 0xFF) != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }
        catch (NoSuchAlgorithmException e) {
            // Every JRE ships SHA-256; this branch exists only to satisfy the checked exception.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
