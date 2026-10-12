package et.medco.etrade.service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UpstreamDatesTest {

    @Test
    void parsesThePortalsUsStyleEnglishDates() {
        assertThat(UpstreamDates.parse("3/28/2019").iso()).isEqualTo(LocalDate.of(2019, 3, 28));
        assertThat(UpstreamDates.parse("11/10/2022").iso()).isEqualTo(LocalDate.of(2022, 11, 10));
        assertThat(UpstreamDates.parse("7/7/2026").iso()).isEqualTo(LocalDate.of(2026, 7, 7));
    }

    @Test
    void keepsTheRawTextAlongsideTheParsedDate() {
        assertThat(UpstreamDates.parse(" 4/24/2026 ").raw()).isEqualTo("4/24/2026");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Data not available", "data not available", "N/A", "-"})
    void treatsThePortalsPlaceholdersAsNoDateWithoutLosingTheText(String placeholder) {
        var parsed = UpstreamDates.parse(placeholder);

        assertThat(parsed.iso()).isNull();
        assertThat(parsed.raw()).isEqualTo(placeholder.strip());
        assertThat(parsed.isUnparsed()).isTrue();
    }

    @Test
    void rejectsEthiopianCalendarDatesRatherThanMisreadingThem() {
        // 19/7/2011 EC is the same day as 3/28/2019; with M/d parsing, month 19 cannot resolve,
        // so it surfaces as raw text instead of a wrong Gregorian date.
        var parsed = UpstreamDates.parse("19/7/2011");

        assertThat(parsed.iso()).isNull();
        assertThat(parsed.raw()).isEqualTo("19/7/2011");
    }

    @Test
    void reportsNothingWhenThePortalSentNothing() {
        assertThat(UpstreamDates.parse(null)).isNull();
        assertThat(UpstreamDates.parse("   ")).isNull();
    }
}
