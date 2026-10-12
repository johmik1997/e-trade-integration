package et.medco.etrade.service;

import java.util.Arrays;

/** A decoded associate photo, addressed by the hash of its own bytes. */
public record StoredPhoto(String id, String contentType, byte[] bytes) {

    public int size() {
        return bytes.length;
    }

    // A record holding an array would otherwise compare and hash by array identity, and
    // would print the array's address; since the id already identifies the content, define
    // all three by hand.

    @Override
    public boolean equals(Object other) {
        return other instanceof StoredPhoto photo
                && id.equals(photo.id)
                && contentType.equals(photo.contentType)
                && Arrays.equals(bytes, photo.bytes);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "StoredPhoto[id=%s, contentType=%s, size=%d]".formatted(id, contentType, size());
    }
}
