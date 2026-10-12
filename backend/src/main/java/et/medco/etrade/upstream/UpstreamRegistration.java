package et.medco.etrade.upstream;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Verbatim shape of {@code GET /api/Registration/GetRegistrationInfoByTin/{tin}/{lang}}.
 * Field names are the upstream's, typo included: the API really does spell it
 * {@code LegalCondtion}. Nothing outside this package should see these names.
 */
public record UpstreamRegistration(
        @JsonProperty("Tin") String tin,
        @JsonProperty("LegalCondtion") String legalCondition,
        @JsonProperty("RegNo") String regNo,
        @JsonProperty("RegDate") String regDate,
        @JsonProperty("BusinessName") String businessName,
        @JsonProperty("BusinessNameAmh") String businessNameAmharic,
        @JsonProperty("PaidUpCapital") BigDecimal paidUpCapital,
        @JsonProperty("AssociateShortInfos") List<UpstreamAssociate> associateShortInfos,
        @JsonProperty("Businesses") List<UpstreamBusiness> businesses) {

    public List<UpstreamAssociate> associateShortInfos() {
        return associateShortInfos == null ? List.of() : associateShortInfos;
    }

    public List<UpstreamBusiness> businesses() {
        return businesses == null ? List.of() : businesses;
    }

    /**
     * The portal answers an unregistered or malformed TIN with {@code 204 No Content}, so an
     * empty body is the usual not-found signal; this guards the case of a body that parses
     * but carries nothing identifying.
     */
    public boolean isEmpty() {
        return (tin == null || tin.isBlank()) && (regNo == null || regNo.isBlank())
                && (businessName == null || businessName.isBlank());
    }
}
