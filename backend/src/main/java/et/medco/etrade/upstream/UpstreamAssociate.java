package et.medco.etrade.upstream;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One manager/owner entry. {@code Photo} is a bare base64 payload — no data-URI prefix —
 * and is typically ~50KB of scanned JPEG, so it never reaches our own JSON contract; see
 * {@code PhotoStore}. {@code Position} and both phone numbers were null in every record
 * sampled from the live portal, but are mapped in case they are populated elsewhere.
 */
public record UpstreamAssociate(
        @JsonProperty("Position") String position,
        @JsonProperty("ManagerName") String managerName,
        @JsonProperty("ManagerNameEng") String managerNameEnglish,
        @JsonProperty("MobilePhone") String mobilePhone,
        @JsonProperty("RegularPhone") String regularPhone,
        @JsonProperty("Photo") String photo) {
}
