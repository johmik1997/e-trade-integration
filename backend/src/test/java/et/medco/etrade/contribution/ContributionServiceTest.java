package et.medco.etrade.contribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import et.medco.etrade.domain.Activity;
import et.medco.etrade.domain.BusinessLicence;
import et.medco.etrade.domain.LegalCondition;
import et.medco.etrade.domain.LocalizedText;
import et.medco.etrade.domain.PortalDate;
import et.medco.etrade.domain.Registration;
import et.medco.etrade.service.RegistrationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ContributionServiceTest {

    private static final ContributionProperties SCHEDULE = new ContributionProperties(
            "ETB",
            Map.of("1", new ContributionProperties.Category("Sole proprietor", new BigDecimal("1000.00")),
                    "2", new ContributionProperties.Category("Private Limited Company", new BigDecimal("5000.00"))),
            new ContributionProperties.Fallback("Standard contribution", new BigDecimal("2500.00")));

    private final RegistrationService registrations = mock(RegistrationService.class);
    private final ContributionService service = new ContributionService(registrations, SCHEDULE);

    private static Registration registration(String legalConditionCode, String... licenceIds) {
        var licences = java.util.Arrays.stream(licenceIds)
                .map(id -> new BusinessLicence(id, "LIC/" + id, LocalizedText.of("MEDCO", "ሜድኮ"),
                        new PortalDate(LocalDate.of(2019, 3, 28), "3/28/2019"), null, null,
                        new PortalDate(LocalDate.of(2026, 7, 7), "7/7/2026"), null,
                        List.of(new Activity(39141, "Software development"))))
                .toList();
        return new Registration("0061688042", "BL/AA/2/0018364/2011",
                new PortalDate(LocalDate.of(2019, 3, 28), "3/28/2019"),
                LocalizedText.of("MEDCO TECHNOLOGY SOLUTIONS PLC", "ሜድኮ"),
                new BigDecimal("5000000.00"),
                legalConditionCode == null ? null : new LegalCondition(legalConditionCode, null),
                List.of(), licences, java.time.Instant.now());
    }

    @Test
    void pricesAPlcFromItsLegalConditionCode() {
        given(registrations.lookup(any(), any())).willReturn(registration("2", "guid-1"));

        var quote = service.quote("0061688042", "guid-1");

        assertThat(quote.category().label()).isEqualTo("Private Limited Company");
        assertThat(quote.category().fee()).isEqualByComparingTo("5000.00");
        assertThat(quote.category().currency()).isEqualTo("ETB");
        assertThat(quote.category().inferred()).isFalse();
        assertThat(quote.licenceNumber()).isEqualTo("LIC/guid-1");
        assertThat(quote.businessName()).isEqualTo("MEDCO TECHNOLOGY SOLUTIONS PLC");
    }

    @Test
    void pricesASoleProprietorDifferently() {
        given(registrations.lookup(any(), any())).willReturn(registration("1", "guid-1"));

        assertThat(service.quote("0061688042", "guid-1").category().fee())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    void fallsBackAndSaysSoForALegalConditionNotInTheSchedule() {
        given(registrations.lookup(any(), any())).willReturn(registration("7", "guid-1"));

        var category = service.quote("0061688042", "guid-1").category();

        assertThat(category.fee()).isEqualByComparingTo("2500.00");
        assertThat(category.label()).isEqualTo("Standard contribution");
        // The UI needs to know the fee was not derived from a known category.
        assertThat(category.inferred()).isTrue();
        assertThat(category.legalConditionCode()).isEqualTo("7");
    }

    @Test
    void fallsBackWhenTheRecordCarriesNoLegalConditionAtAll() {
        given(registrations.lookup(any(), any())).willReturn(registration(null, "guid-1"));

        var category = service.quote("0061688042", "guid-1").category();

        assertThat(category.inferred()).isTrue();
        assertThat(category.legalConditionCode()).isNull();
    }

    @Test
    void pricesEveryLicenceSoThePayerCanChoose() {
        given(registrations.lookup(any(), any())).willReturn(registration("2", "guid-1", "guid-2", "guid-3"));

        var quotes = service.quoteAll("0061688042");

        assertThat(quotes).hasSize(3)
                .allSatisfy(quote -> assertThat(quote.category().fee()).isEqualByComparingTo("5000.00"))
                .extracting(FeeQuote::licenceId)
                .containsExactly("guid-1", "guid-2", "guid-3");
    }

    @Test
    void pricesOnlyTheSelectedLicencesInTheRecordsOwnOrder() {
        given(registrations.lookup(any(), any())).willReturn(registration("2", "guid-1", "guid-2", "guid-3"));

        // Supplied out of order on purpose: the result should follow the record, not the request.
        var quotes = service.quote("0061688042", List.of("guid-3", "guid-1"));

        assertThat(quotes).extracting(FeeQuote::licenceId).containsExactly("guid-1", "guid-3");
    }

    @Test
    void rejectsTheWholeSelectionIfAnyLicenceIsNotOnTheRecord() {
        given(registrations.lookup(any(), any())).willReturn(registration("2", "guid-1"));

        // Quoting the valid subset would under-charge against what the payer selected.
        assertThatThrownBy(() -> service.quote("0061688042", List.of("guid-1", "not-mine")))
                .isInstanceOf(LicenceNotFoundException.class);
    }

    @Test
    void rejectsAnEmptySelection() {
        assertThatThrownBy(() -> service.quote("0061688042", List.of()))
                .isInstanceOf(NoLicenceSelectedException.class);
    }

    @Test
    void rejectsALicenceThatIsNotOnTheRecord() {
        given(registrations.lookup(any(), any())).willReturn(registration("2", "guid-1"));

        assertThatThrownBy(() -> service.quote("0061688042", "not-mine"))
                .isInstanceOf(LicenceNotFoundException.class)
                .hasMessageContaining("not-mine");
    }
}
