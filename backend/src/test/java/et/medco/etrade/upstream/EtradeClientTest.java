package et.medco.etrade.upstream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import et.medco.etrade.TestProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.FORBIDDEN;

class EtradeClientTest {

    private static final String URL =
            "https://etrade.example/api/Registration/GetRegistrationInfoByTin/0061688042/en";

    private final RestClient.Builder builder = RestClient.builder()
            .baseUrl("https://etrade.example")
            .defaultHeader("Referer", TestProperties.properties().referer());
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final EtradeClient client = new EtradeClient(builder.build(), TestProperties.properties());

    private static String sampleJson() throws IOException {
        return new ClassPathResource("sample-registration.json")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void readsTheSampleResponseIncludingTheMisspelledLegalCondtionField() throws IOException {
        // The portal's WAF answers 417 to any call without a Referer on its own origin.
        server.expect(requestTo(URL)).andExpect(method(GET))
                .andExpect(header("Referer", "https://etrade.example/"))
                .andRespond(withSuccess(sampleJson(), MediaType.APPLICATION_JSON));

        var body = client.fetchByTin("0061688042", "en");

        assertThat(body.tin()).isEqualTo("0061688042");
        assertThat(body.legalCondition()).isEqualTo("2");
        assertThat(body.businessNameAmharic()).startsWith("ሜድኮ");
        assertThat(body.associateShortInfos()).hasSize(1);
        server.verify();
    }

    @Test
    void treatsAnAllNullBodyAsNotFound() {
        // The portal answers unknown TINs with 200 and nulls, not with a 404.
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"Tin\":null,\"RegNo\":null,\"BusinessName\":null}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchByTin("0061688042", "en"))
                .isInstanceOf(RegistrationNotFoundException.class);
    }

    @Test
    void treatsAnEmptyBodyAsNotFound() {
        server.expect(requestTo(URL)).andRespond(withSuccess());

        assertThatThrownBy(() -> client.fetchByTin("0061688042", "en"))
                .isInstanceOf(RegistrationNotFoundException.class);
    }

    @Test
    void mapsUpstream404ToNotFoundWithoutRetrying() {
        server.expect(requestTo(URL)).andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.fetchByTin("0061688042", "en"))
                .isInstanceOf(RegistrationNotFoundException.class);
        server.verify();
    }

    @Test
    void mapsOtherClientErrorsToRejectedWithoutRetrying() {
        server.expect(requestTo(URL)).andRespond(withStatus(FORBIDDEN));

        assertThatThrownBy(() -> client.fetchByTin("0061688042", "en"))
                .isInstanceOf(UpstreamRejectedException.class)
                .extracting(e -> ((UpstreamRejectedException) e).status())
                .isEqualTo(403);
        server.verify();
    }

    @Test
    void retriesServerErrorsAndSucceedsOnTheSecondAttempt() throws IOException {
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL))
                .andRespond(withSuccess(sampleJson(), MediaType.APPLICATION_JSON));

        assertThat(client.fetchByTin("0061688042", "en").regNo()).isEqualTo("BL/AA/2/0018364/2011");
        server.verify();
    }

    @Test
    void givesUpAsUnavailableOnceRetriesAreExhausted() {
        var exhausting = new EtradeClient(builder.build(), TestProperties.properties(1, Map.of()));
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> exhausting.fetchByTin("0061688042", "en"))
                .isInstanceOf(UpstreamUnavailableException.class);
        server.verify();
    }
}
