package et.medco.etrade.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import et.medco.etrade.config.EtradeProperties;
import et.medco.etrade.domain.Activity;
import et.medco.etrade.domain.Associate;
import et.medco.etrade.domain.BusinessLicence;
import et.medco.etrade.domain.LegalCondition;
import et.medco.etrade.domain.LocalizedText;
import et.medco.etrade.domain.Registration;
import et.medco.etrade.upstream.UpstreamAssociate;
import et.medco.etrade.upstream.UpstreamBusiness;
import et.medco.etrade.upstream.UpstreamRegistration;
import et.medco.etrade.upstream.UpstreamSubGroup;

/** Translates the portal's payload into our contract, stashing photos as it goes. */
@Component
public class RegistrationMapper {

    private final PhotoStore photoStore;
    private final EtradeProperties properties;

    RegistrationMapper(PhotoStore photoStore, EtradeProperties properties) {
        this.photoStore = photoStore;
        this.properties = properties;
    }

    public Registration toRegistration(UpstreamRegistration upstream) {
        var associates = new ArrayList<Associate>(upstream.associateShortInfos().size());
        for (UpstreamAssociate associate : upstream.associateShortInfos()) {
            associates.add(toAssociate(associate));
        }
        var licences = new ArrayList<BusinessLicence>(upstream.businesses().size());
        for (UpstreamBusiness business : upstream.businesses()) {
            licences.add(toLicence(business));
        }
        return new Registration(
                trimToNull(upstream.tin()),
                trimToNull(upstream.regNo()),
                UpstreamDates.parse(upstream.regDate()),
                LocalizedText.of(upstream.businessName(), upstream.businessNameAmharic()),
                upstream.paidUpCapital(),
                toLegalCondition(upstream.legalCondition()),
                List.copyOf(associates),
                List.copyOf(licences),
                Instant.now());
    }

    private Associate toAssociate(UpstreamAssociate upstream) {
        String photoUrl = photoStore.store(upstream.photo())
                .map(photo -> "/api/v1/photos/" + photo.id())
                .orElse(null);
        return new Associate(
                trimToNull(upstream.position()),
                LocalizedText.of(upstream.managerNameEnglish(), upstream.managerName()),
                trimToNull(upstream.mobilePhone()),
                trimToNull(upstream.regularPhone()),
                photoUrl);
    }

    private BusinessLicence toLicence(UpstreamBusiness upstream) {
        var activities = new ArrayList<Activity>(upstream.subGroups().size());
        for (UpstreamSubGroup subGroup : upstream.subGroups()) {
            // Descriptions routinely arrive with a trailing newline.
            activities.add(new Activity(subGroup.code(), trimToNull(subGroup.description())));
        }
        return new BusinessLicence(
                trimToNull(upstream.mainGuid()),
                trimToNull(upstream.licenceNumber()),
                LocalizedText.of(upstream.tradesName(), upstream.tradeNameAmharic()),
                UpstreamDates.parse(upstream.dateRegistered()),
                UpstreamDates.parse(upstream.renewalDate()),
                UpstreamDates.parse(upstream.renewedFrom()),
                UpstreamDates.parse(upstream.renewedTo()),
                trimToNull(upstream.licensingGroup()),
                List.copyOf(activities));
    }

    private LegalCondition toLegalCondition(String code) {
        String trimmed = trimToNull(code);
        if (trimmed == null) {
            return null;
        }
        return new LegalCondition(trimmed, properties.legalConditions().get(trimmed));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
