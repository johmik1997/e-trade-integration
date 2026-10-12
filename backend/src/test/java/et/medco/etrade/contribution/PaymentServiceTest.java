package et.medco.etrade.contribution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private final PaymentService service = new PaymentService();

    private static FeeQuote quote(String licenceId) {
        return quote(licenceId, "5000.00");
    }

    private static FeeQuote quote(String licenceId, String fee) {
        return new FeeQuote("0061688042", "MEDCO TECHNOLOGY SOLUTIONS PLC", licenceId,
                "LIC/" + licenceId,
                new ContributionCategory("2", "Private Limited Company",
                        new BigDecimal(fee), "ETB", false),
                Instant.now());
    }

    @Test
    void initiatesAPendingPaymentCarryingTheQuotedAmount() {
        var payment = service.initiate(List.of(quote("guid-1")), PaymentMethod.TELEBIRR);

        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.total()).isEqualByComparingTo("5000.00");
        assertThat(payment.lines()).singleElement()
                .satisfies(line -> assertThat(line.licenceNumber()).isEqualTo("LIC/guid-1"));
        assertThat(payment.currency()).isEqualTo("ETB");
        assertThat(payment.method()).isEqualTo(PaymentMethod.TELEBIRR);
        assertThat(payment.settledAt()).isNull();
        assertThat(payment.reference()).startsWith("CNT-").hasSize(19);   // CNT- + yyyyMMdd + - + 6
    }

    @Test
    void settlesSeveralSelectedLicencesUnderOneReference() {
        var payment = service.initiate(
                List.of(quote("guid-1", "5000.00"), quote("guid-2", "5000.00"), quote("guid-3", "1000.00")),
                PaymentMethod.TELEBIRR);

        assertThat(payment.total()).isEqualByComparingTo("11000.00");
        assertThat(payment.lines()).hasSize(3)
                .extracting(PaymentLine::licenceId)
                .containsExactly("guid-1", "guid-2", "guid-3");
    }

    @Test
    void refusesAPaymentThatCoversNothing() {
        assertThatThrownBy(() -> service.initiate(List.of(), PaymentMethod.CASH))
                .isInstanceOf(NoLicenceSelectedException.class);
    }

    @Test
    void issuesAnUnambiguousReferencePerPayment() {
        var first = service.initiate(List.of(quote("guid-1")), PaymentMethod.CBE_BIRR).reference();
        var second = service.initiate(List.of(quote("guid-1")), PaymentMethod.CBE_BIRR).reference();

        assertThat(first).isNotEqualTo(second);
        // No I/O/0/1 in the alphabet: these get read aloud and typed in from receipts.
        assertThat(first.substring(13)).doesNotContainAnyWhitespaces().matches("[A-HJ-NP-Z2-9]{6}");
    }

    @Test
    void settlesAPendingPayment() {
        var reference = service.initiate(List.of(quote("guid-1")), PaymentMethod.CASH).reference();

        var settled = service.confirm(reference);

        assertThat(settled.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(settled.settledAt()).isNotNull();
        assertThat(service.require(reference).status()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void treatsASecondConfirmationAsANoOpRatherThanReStampingIt() {
        var reference = service.initiate(List.of(quote("guid-1")), PaymentMethod.CASH).reference();
        var first = service.confirm(reference);

        var again = service.confirm(reference);

        assertThat(again).isEqualTo(first);
        assertThat(again.settledAt()).isEqualTo(first.settledAt());
    }

    @Test
    void refusesToConfirmAReferenceItNeverIssued() {
        assertThatThrownBy(() -> service.confirm("CNT-20261012-ZZZZZZ"))
                .isInstanceOf(PaymentNotFoundException.class);
        assertThatThrownBy(() -> service.require("nope"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void listsATinsPaymentsNewestFirst() throws InterruptedException {
        var older = service.initiate(List.of(quote("guid-1")), PaymentMethod.CASH).reference();
        Thread.sleep(5);
        var newer = service.initiate(List.of(quote("guid-2")), PaymentMethod.TELEBIRR).reference();

        assertThat(service.forTin("0061688042")).extracting(Payment::reference)
                .containsExactly(newer, older);
        assertThat(service.forTin("9999999999")).isEmpty();
    }
}
