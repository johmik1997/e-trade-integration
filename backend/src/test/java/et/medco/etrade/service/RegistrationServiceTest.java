package et.medco.etrade.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import et.medco.etrade.TestProperties;
import et.medco.etrade.config.CacheConfig;
import et.medco.etrade.upstream.EtradeClient;
import et.medco.etrade.upstream.RegistrationNotFoundException;
import et.medco.etrade.upstream.UpstreamAssociate;
import et.medco.etrade.upstream.UpstreamRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RegistrationServiceTest {

    private static final String PHOTO =
            java.util.Base64.getEncoder().encodeToString(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 7});

    private final et.medco.etrade.config.EtradeProperties properties = TestProperties.properties();
    private final EtradeClient client = mock(EtradeClient.class);
    private final PhotoStore photoStore = spy(new PhotoStore(properties));
    private final RegistrationMapper mapper = new RegistrationMapper(photoStore, properties);
    private final org.springframework.cache.CacheManager cacheManager = cacheManager();
    private final RegistrationService service =
            new RegistrationService(client, mapper, photoStore, cacheManager, properties);

    /** Same cache semantics as production, built here so the config class stays package-private. */
    private static org.springframework.cache.CacheManager cacheManager() {
        return new org.springframework.cache.concurrent.ConcurrentMapCacheManager(CacheConfig.REGISTRATIONS);
    }

    private static UpstreamRegistration upstream() {
        return new UpstreamRegistration("0061688042", "2", "BL/AA/2/0018364/2011", "3/28/2019",
                "MEDCO TECHNOLOGY SOLUTIONS PLC", "ሜድኮ", new BigDecimal("5000000.00"),
                List.of(new UpstreamAssociate(null, "ሰመረ ጻድቅ", "SEMERE TSADIK", null, null, PHOTO)),
                List.of());
    }

    @Test
    void callsThePortalOnceAndServesTheSecondLookupFromCache() {
        given(client.fetchByTin("0061688042", "en")).willReturn(upstream());

        var first = service.lookup("0061688042", "en");
        var second = service.lookup("0061688042", "en");

        assertThat(second).isSameAs(first);
        verify(client, times(1)).fetchByTin("0061688042", "en");
    }

    @Test
    void cachesPerLanguage() {
        given(client.fetchByTin(eq("0061688042"), any())).willReturn(upstream());

        service.lookup("0061688042", "en");
        service.lookup("0061688042", "am");

        verify(client).fetchByTin("0061688042", "en");
        verify(client).fetchByTin("0061688042", "am");
    }

    @Test
    void refetchesWhenTheCachedEntryPointsAtPhotosThatHaveExpired() {
        given(client.fetchByTin("0061688042", "en")).willReturn(upstream());
        service.lookup("0061688042", "en");

        // Simulate the photo store expiring ahead of the registration cache.
        willReturn(Optional.empty()).given(photoStore).find(any());
        service.lookup("0061688042", "en");

        verify(client, times(2)).fetchByTin("0061688042", "en");
    }

    @Test
    void normalizesPastedTinsAndDefaultsTheLanguage() {
        given(client.fetchByTin("0061688042", "en")).willReturn(upstream());

        var registration = service.lookup(" 0061-688-042 ", null);

        assertThat(registration.tin()).isEqualTo("0061688042");
        verify(client).fetchByTin("0061688042", "en");
    }

    @Test
    void rejectsMalformedTinsWithoutTouchingThePortal() {
        assertThatThrownBy(() -> service.lookup("12345", "en")).isInstanceOf(InvalidTinException.class);
        assertThatThrownBy(() -> service.lookup("abcdefghij", "en")).isInstanceOf(InvalidTinException.class);
        assertThatThrownBy(() -> service.lookup(null, "en")).isInstanceOf(InvalidTinException.class);

        verify(client, never()).fetchByTin(any(), any());
    }

    @Test
    void rejectsUnsupportedLanguagesWithoutTouchingThePortal() {
        assertThatThrownBy(() -> service.lookup("0061688042", "fr"))
                .isInstanceOf(UnsupportedLanguageException.class);

        verify(client, never()).fetchByTin(any(), any());
    }

    @Test
    void doesNotCacheNotFoundOutcomes() {
        given(client.fetchByTin("0061688042", "en"))
                .willThrow(new RegistrationNotFoundException("0061688042"));

        assertThatThrownBy(() -> service.lookup("0061688042", "en"))
                .isInstanceOf(RegistrationNotFoundException.class);
        assertThatThrownBy(() -> service.lookup("0061688042", "en"))
                .isInstanceOf(RegistrationNotFoundException.class);

        verify(client, times(2)).fetchByTin("0061688042", "en");
    }

    @Test
    void mapsLegalConditionLabelsFromConfiguration() {
        var labelled = TestProperties.properties(1, Map.of("2", "PLC"));
        var labellingService = new RegistrationService(client, new RegistrationMapper(photoStore, labelled),
                photoStore, cacheManager(), labelled);
        given(client.fetchByTin("0061688042", "en")).willReturn(upstream());

        assertThat(labellingService.lookup("0061688042", "en").legalCondition().label()).isEqualTo("PLC");
    }
}
