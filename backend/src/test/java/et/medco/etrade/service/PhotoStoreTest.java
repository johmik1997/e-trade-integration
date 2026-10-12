package et.medco.etrade.service;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import et.medco.etrade.TestProperties;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoStoreTest {

    private final PhotoStore store = new PhotoStore(TestProperties.properties());

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    @Test
    void storesJpegAndAddressesItByContentHash() {
        var first = store.store(Base64.getEncoder().encodeToString(JPEG)).orElseThrow();
        var second = store.store(Base64.getEncoder().encodeToString(JPEG)).orElseThrow();

        assertThat(first.contentType()).isEqualTo("image/jpeg");
        assertThat(first.bytes()).isEqualTo(JPEG);
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(store.find(first.id())).contains(first);
    }

    @Test
    void toleratesLineBreaksAndDataUriPrefixes() {
        String encoded = Base64.getMimeEncoder().encodeToString(JPEG);

        assertThat(store.store(encoded)).isPresent();
        assertThat(store.store("data:image/jpeg;base64," + encoded)).isPresent();
    }

    @Test
    void labelsUnknownBytesAsOctetStream() {
        var stored = store.store(Base64.getEncoder().encodeToString(new byte[] {1, 2, 3, 4})).orElseThrow();

        assertThat(stored.contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void skipsAbsentOrUndecodablePayloads() {
        assertThat(store.store(null)).isEmpty();
        assertThat(store.store("   ")).isEmpty();
        assertThat(store.store("!!!not base64!!!")).isEmpty();
    }

    @Test
    void reportsNothingForAnUnknownId() {
        assertThat(store.find("deadbeef")).isEmpty();
    }
}
