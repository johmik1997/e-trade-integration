package et.medco.etrade.contribution;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import et.medco.etrade.domain.BusinessLicence;
import et.medco.etrade.domain.LegalCondition;
import et.medco.etrade.domain.Registration;
import et.medco.etrade.service.RegistrationService;

/**
 * Prices a contribution for a licence. The category comes from the registration's
 * {@code LegalCondtion}; the licence identifies what is being paid for.
 */
@Service
public class ContributionService {

    private final RegistrationService registrations;
    private final ContributionProperties properties;

    ContributionService(RegistrationService registrations, ContributionProperties properties) {
        this.registrations = registrations;
        this.properties = properties;
    }

    /** Every licence on the record, each already priced, for the payer to choose from. */
    public List<FeeQuote> quoteAll(String tin) {
        Registration registration = registrations.lookup(tin, null);
        return registration.licences().stream()
                .map(licence -> toQuote(registration, licence))
                .toList();
    }

    public FeeQuote quote(String tin, String licenceId) {
        return quote(tin, List.of(licenceId)).getFirst();
    }

    /**
     * Prices the selected licences. Every id must be on the record — an unknown one is an
     * error rather than a silent omission, so a payer is never quoted less than they chose.
     * The result follows the record's own licence order, not the order ids were supplied in.
     */
    public List<FeeQuote> quote(String tin, List<String> licenceIds) {
        if (licenceIds == null || licenceIds.isEmpty()) {
            throw new NoLicenceSelectedException();
        }
        Registration registration = registrations.lookup(tin, null);
        var selected = Set.copyOf(licenceIds);
        var known = registration.licences().stream()
                .map(BusinessLicence::id)
                .collect(java.util.stream.Collectors.toSet());
        for (String id : selected) {
            if (!known.contains(id)) {
                throw new LicenceNotFoundException(registration.tin(), id);
            }
        }
        return registration.licences().stream()
                .filter(licence -> selected.contains(licence.id()))
                .map(licence -> toQuote(registration, licence))
                .toList();
    }

    private FeeQuote toQuote(Registration registration, BusinessLicence licence) {
        return new FeeQuote(
                registration.tin(),
                registration.businessName().preferred(),
                licence.id(),
                licence.licenceNumber(),
                categoryFor(registration.legalCondition()),
                java.time.Instant.now());
    }

    /**
     * Maps a legal condition onto a fee band. An unknown or absent code falls back to the
     * standard band and is marked {@code inferred}, so the UI can show that the fee was not
     * derived from a known category rather than implying the registry priced it.
     */
    ContributionCategory categoryFor(LegalCondition legalCondition) {
        String code = legalCondition == null ? null : legalCondition.code();
        var configured = code == null ? null : properties.categories().get(code);
        if (configured == null) {
            return new ContributionCategory(code, properties.fallback().label(),
                    properties.fallback().fee(), properties.currency(), true);
        }
        return new ContributionCategory(code, configured.label(), configured.fee(),
                properties.currency(), false);
    }
}
