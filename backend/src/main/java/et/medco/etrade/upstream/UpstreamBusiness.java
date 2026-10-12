package et.medco.etrade.upstream;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One trade licence held by the taxpayer. The date fields are strings and are not always
 * dates: the portal writes the literal {@code "Data not available"} where a renewal has
 * never happened.
 */
public record UpstreamBusiness(
        @JsonProperty("MainGuid") String mainGuid,
        @JsonProperty("OwnerTIN") String ownerTin,
        @JsonProperty("LicenceNumber") String licenceNumber,
        @JsonProperty("TradesName") String tradesName,
        @JsonProperty("TradeNameAmh") String tradeNameAmharic,
        @JsonProperty("DateRegistered") String dateRegistered,
        @JsonProperty("RenewalDate") String renewalDate,
        @JsonProperty("RenewedFrom") String renewedFrom,
        @JsonProperty("RenewedTo") String renewedTo,
        @JsonProperty("BusinessLicensingGroupMain") String licensingGroup,
        @JsonProperty("SubGroups") List<UpstreamSubGroup> subGroups) {

    public List<UpstreamSubGroup> subGroups() {
        return subGroups == null ? List.of() : subGroups;
    }
}
