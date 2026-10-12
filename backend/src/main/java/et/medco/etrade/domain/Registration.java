package et.medco.etrade.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Our own contract: stable field names, ISO dates, no base64, explicit nulls.
 *
 * @param licences    every trade licence on the record; empty for a taxpayer with none
 * @param retrievedAt when this snapshot came off the portal (cached responses keep their original stamp)
 */
public record Registration(
        String tin,
        String registrationNumber,
        PortalDate registrationDate,
        LocalizedText businessName,
        BigDecimal paidUpCapital,
        LegalCondition legalCondition,
        List<Associate> associates,
        List<BusinessLicence> licences,
        Instant retrievedAt) {
}
