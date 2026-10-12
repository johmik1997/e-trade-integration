package et.medco.etrade.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import et.medco.etrade.TestProperties;
import et.medco.etrade.config.EtradeProperties;
import et.medco.etrade.domain.Activity;
import et.medco.etrade.domain.Associate;
import et.medco.etrade.domain.BusinessLicence;
import et.medco.etrade.domain.LegalCondition;
import et.medco.etrade.domain.LocalizedText;
import et.medco.etrade.domain.PortalDate;
import et.medco.etrade.domain.Registration;
import et.medco.etrade.service.InvalidTinException;
import et.medco.etrade.service.RegistrationService;
import et.medco.etrade.upstream.RegistrationNotFoundException;
import et.medco.etrade.upstream.UpstreamUnavailableException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
@Import(RegistrationControllerTest.Properties.class)
class RegistrationControllerTest {

    @TestConfiguration
    static class Properties {
        @Bean
        EtradeProperties etradeProperties() {
            return TestProperties.properties();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService service;

    private static Registration registration() {
        return new Registration("0061688042", "BL/AA/2/0018364/2011",
                new PortalDate(LocalDate.of(2019, 3, 28), "3/28/2019"),
                LocalizedText.of("MEDCO TECHNOLOGY SOLUTIONS PLC", "ሜድኮ"),
                new BigDecimal("5000000.00"), new LegalCondition("2", null),
                List.of(new Associate(null, LocalizedText.of("SEMERE TSADIK", "ሰመረ ጻድቅ"),
                        null, null, "/api/v1/photos/abc123")),
                List.of(new BusinessLicence("guid-1", "BL/AA/14/673/2546312/2011",
                        LocalizedText.of("MEDCO TECHNOLOGY SOLUTIONS PLC", "ሜድኮ"),
                        new PortalDate(LocalDate.of(2019, 3, 28), "3/28/2019"),
                        new PortalDate(null, "Data not available"),
                        new PortalDate(LocalDate.of(2025, 7, 8), "7/8/2025"),
                        new PortalDate(LocalDate.of(2026, 7, 7), "7/7/2026"),
                        null,
                        List.of(new Activity(39141, "Software development")))),
                Instant.parse("2026-10-12T09:00:00Z"));
    }

    @Test
    void returnsIsoDatesAndPhotoLinksRatherThanBase64() throws Exception {
        given(service.lookup("0061688042", null)).willReturn(registration());

        mockMvc.perform(get("/api/v1/registrations/0061688042"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.tin").value("0061688042"))
                .andExpect(jsonPath("$.registrationDate.iso").value("2019-03-28"))
                .andExpect(jsonPath("$.registrationDate.raw").value("3/28/2019"))
                .andExpect(jsonPath("$.businessName.en").value("MEDCO TECHNOLOGY SOLUTIONS PLC"))
                .andExpect(jsonPath("$.paidUpCapital").value(5000000.00))
                .andExpect(jsonPath("$.associates[0].name.am").value("ሰመረ ጻድቅ"))
                .andExpect(jsonPath("$.associates[0].photoUrl").value("/api/v1/photos/abc123"))
                .andExpect(jsonPath("$.associates[0].position").doesNotExist())
                .andExpect(jsonPath("$.legalCondition.label").doesNotExist())
                .andExpect(jsonPath("$.licences[0].licenceNumber").value("BL/AA/14/673/2546312/2011"))
                .andExpect(jsonPath("$.licences[0].renewedTo.iso").value("2026-07-07"))
                .andExpect(jsonPath("$.licences[0].activities[0].code").value(39141))
                // A licence never renewed keeps the portal's own words rather than a null date.
                .andExpect(jsonPath("$.licences[0].renewalDate.iso").doesNotExist())
                .andExpect(jsonPath("$.licences[0].renewalDate.raw").value("Data not available"));
    }

    @Test
    void passesTheLanguageThrough() throws Exception {
        given(service.lookup("0061688042", "am")).willReturn(registration());

        mockMvc.perform(get("/api/v1/registrations/0061688042").param("lang", "am"))
                .andExpect(status().isOk());
    }

    @Test
    void answers400WithAProblemDocumentForABadTin() throws Exception {
        given(service.lookup(any(), any())).willThrow(new InvalidTinException("A TIN must be exactly 10 digits"));

        mockMvc.perform(get("/api/v1/registrations/123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid TIN"));
    }

    @Test
    void answers404ForAnUnknownTin() throws Exception {
        given(service.lookup(any(), any())).willThrow(new RegistrationNotFoundException("0000000000"));

        mockMvc.perform(get("/api/v1/registrations/0000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.tin").value("0000000000"));
    }

    @Test
    void answers503WhenThePortalIsDownRatherThanLeakingA500() throws Exception {
        given(service.lookup(any(), any()))
                .willThrow(new UpstreamUnavailableException("e-Trade is unreachable", null));

        mockMvc.perform(get("/api/v1/registrations/0061688042"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("e-Trade is unavailable"));
    }
}
