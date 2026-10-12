package et.medco.etrade.domain;

import java.util.List;

/**
 * One trade licence. A taxpayer commonly holds several — the sample company holds three,
 * covering software development, medical-supply import and diagnostic lab services.
 *
 * @param id            the portal's {@code MainGuid} for this licence
 * @param renewedFrom   start of the period the licence is currently renewed for
 * @param renewedTo     end of that period, i.e. when it next needs renewing
 */
public record BusinessLicence(
        String id,
        String licenceNumber,
        LocalizedText tradeName,
        PortalDate registeredOn,
        PortalDate renewalDate,
        PortalDate renewedFrom,
        PortalDate renewedTo,
        String licensingGroup,
        List<Activity> activities) {
}
