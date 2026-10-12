package et.medco.etrade;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Guards the wiring: properties bind, caches build, controllers map. No network involved. */
@SpringBootTest(properties = "etrade.base-url=https://etrade.invalid")
class EtradeIntegrationApplicationTests {

    @Test
    void contextLoads() {
    }
}
