package et.medco.etrade.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the CORS mapping, which is easy to get subtly wrong: browsers attach an Origin
 * header to same-origin POST requests as well as cross-origin ones, so a GET-only mapping
 * makes Spring answer 403 "Invalid CORS request" to the payment calls even through the dev
 * proxy, where nothing looks cross-origin at all.
 */
@SpringBootTest(properties = "etrade.base-url=https://etrade.invalid")
class CorsConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void allowsThePreflightForAPaymentPost() throws Exception {
        mockMvc.perform(options("/api/v1/payments")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void doesNotRejectAPostThatCarriesAnOrigin() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("Origin", "http://localhost:5173")
                        .contentType("application/json")
                        .content("{\"tin\":\"0061688042\",\"licenceIds\":[\"x\"],\"method\":\"CASH\"}"))
                // Whatever the request's own fate, it must not be turned away as a CORS violation.
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 403) {
                        throw new AssertionError("CORS rejected a same-origin POST: "
                                + result.getResponse().getContentAsString());
                    }
                });
    }

    @Test
    void refusesAnOriginThatIsNotConfigured() throws Exception {
        mockMvc.perform(options("/api/v1/payments")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
