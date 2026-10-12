package et.medco.etrade.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import et.medco.etrade.TestProperties;
import et.medco.etrade.config.EtradeProperties;
import et.medco.etrade.domain.Registration;
import et.medco.etrade.upstream.UpstreamAssociate;
import et.medco.etrade.upstream.UpstreamBusiness;
import et.medco.etrade.upstream.UpstreamRegistration;
import et.medco.etrade.upstream.UpstreamSubGroup;

import static org.assertj.core.api.Assertions.assertThat;

/** Driven by a payload captured from the live portal, photo swapped for a tiny stand-in. */
class RegistrationMapperTest {

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final EtradeProperties properties = TestProperties.properties(1, Map.of("2", "Private Limited Company"));
    private final PhotoStore photoStore = new PhotoStore(properties);
    private final RegistrationMapper mapper = new RegistrationMapper(photoStore, properties);

    private Registration liveSample() throws IOException {
        var upstream = JSON.readValue(
                new ClassPathResource("sample-registration.json").getContentAsString(StandardCharsets.UTF_8),
                UpstreamRegistration.class);
        return mapper.toRegistration(upstream);
    }

    @Test
    void mapsTheHeaderFieldsOfTheLivePayload() throws IOException {
        var registration = liveSample();

        assertThat(registration.tin()).isEqualTo("0061688042");
        assertThat(registration.registrationNumber()).isEqualTo("BL/AA/2/0018364/2011");
        assertThat(registration.registrationDate().iso()).isEqualTo(LocalDate.of(2019, 3, 28));
        assertThat(registration.registrationDate().raw()).isEqualTo("3/28/2019");
        assertThat(registration.businessName().en()).isEqualTo("MEDCO TECHNOLOGY SOLUTIONS PLC");
        assertThat(registration.businessName().am()).startsWith("ሜድኮ");
        assertThat(registration.paidUpCapital()).isEqualByComparingTo("5000000.00");
        assertThat(registration.legalCondition().code()).isEqualTo("2");
        assertThat(registration.legalCondition().label()).isEqualTo("Private Limited Company");
    }

    @Test
    void mapsEveryTradeLicenceOnTheRecord() throws IOException {
        var licences = liveSample().licences();

        assertThat(licences).hasSize(3);
        var first = licences.getFirst();
        assertThat(first.licenceNumber()).isEqualTo("BL/AA/14/673/2546312/2011");
        assertThat(first.id()).isEqualTo("7e7e0c7b-f9ed-4b34-a506-80ac4c6562eb");
        assertThat(first.tradeName().en()).isEqualTo("MEDCO TECHNOLOGY SOLUTIONS PLC");
        assertThat(first.registeredOn().iso()).isEqualTo(LocalDate.of(2019, 3, 28));
        assertThat(first.renewedFrom().iso()).isEqualTo(LocalDate.of(2025, 7, 8));
        assertThat(first.renewedTo().iso()).isEqualTo(LocalDate.of(2026, 7, 7));
        assertThat(first.renewalDate().iso()).isEqualTo(LocalDate.of(2026, 4, 24));

        assertThat(licences).extracting(licence -> licence.activities().getFirst().description())
                .containsExactly(
                        "Software development (including design, enrichment and implementation)",
                        "Import trade in human health medical supplies and equipments",
                        "Diagnostic laboratory service");
        assertThat(licences.getFirst().activities().getFirst().code()).isEqualTo(39141);
    }

    @Test
    void trimsTheTrailingNewlineThePortalPutsOnActivityDescriptions() throws IOException {
        assertThat(liveSample().licences().getFirst().activities().getFirst().description())
                .doesNotContain("\n")
                .endsWith("implementation)");
    }

    @Test
    void liftsThePhotoOutOfTheResponseAndLinksToIt() throws IOException {
        var associate = liveSample().associates().getFirst();

        assertThat(associate.name().en()).isEqualTo("SEMERE TSADIK");
        assertThat(associate.name().am()).isEqualTo("ሰመረ ጻድቅ");
        assertThat(associate.position()).isNull();
        assertThat(associate.mobilePhone()).isNull();
        assertThat(associate.photoUrl()).startsWith("/api/v1/photos/");
        assertThat(photoStore.find(associate.photoUrl().substring("/api/v1/photos/".length()))).isPresent();
    }

    @Test
    void keepsALicenceThatHasNeverBeenRenewedRatherThanDroppingIt() {
        // The portal writes "Data not available" into date fields in this case.
        var upstream = new UpstreamRegistration("1234567890", "1", "AM/NS/ATAYE/1/0000195/2007",
                "5/1/2015", "SOLE TRADER", "ነጋዴ", new BigDecimal("600.00"), List.of(),
                List.of(new UpstreamBusiness("guid-1", "1234567890", "04/294/826/2009", "SOLE TRADER",
                        "ነጋዴ", "3/17/2017", "Data not available", "Data not available", "7/7/2018",
                        null, List.of(new UpstreamSubGroup(71116, "Water transport service")))));

        var licence = mapper.toRegistration(upstream).licences().getFirst();

        assertThat(licence.renewalDate().iso()).isNull();
        assertThat(licence.renewalDate().raw()).isEqualTo("Data not available");
        assertThat(licence.renewedTo().iso()).isEqualTo(LocalDate.of(2018, 7, 7));
        assertThat(licence.activities()).singleElement()
                .satisfies(activity -> assertThat(activity.code()).isEqualTo(71116));
    }

    @Test
    void handlesARecordWithNoLicencesNoAssociatesAndNoSubGroups() {
        var upstream = new UpstreamRegistration("0000000000", "1", "SM/JCCC05/1/8814026/2015",
                "11/10/2022", "TAMENECH KAFE CHUFARU", "ታመነች ካፌ ጩፋሩ", new BigDecimal("10000.00"),
                null, null);

        var registration = mapper.toRegistration(upstream);

        assertThat(registration.associates()).isEmpty();
        assertThat(registration.licences()).isEmpty();
        assertThat(registration.registrationDate().iso()).isEqualTo(LocalDate.of(2022, 11, 10));
    }

    @Test
    void labelsTheLegalConditionOnlyFromConfiguredCodes() throws IOException {
        var unmapped = new RegistrationMapper(photoStore, TestProperties.properties()).toRegistration(
                JSON.readValue(new ClassPathResource("sample-registration.json")
                        .getContentAsString(StandardCharsets.UTF_8), UpstreamRegistration.class));

        assertThat(unmapped.legalCondition().code()).isEqualTo("2");
        assertThat(unmapped.legalCondition().label()).isNull();
    }

    @Test
    void leavesPhotoUrlNullWhenTheAssociateHasNoUsablePhoto() {
        var upstream = new UpstreamRegistration("0061688042", "2", "BL/AA/2/0018364/2011", "3/28/2019",
                "MEDCO TECHNOLOGY SOLUTIONS PLC", "ሜድኮ", new BigDecimal("5000000.00"),
                List.of(new UpstreamAssociate(null, "ሰመረ ጻድቅ", "SEMERE TSADIK", null, null, null)),
                List.of());

        assertThat(mapper.toRegistration(upstream).associates().getFirst().photoUrl()).isNull();
    }
}
