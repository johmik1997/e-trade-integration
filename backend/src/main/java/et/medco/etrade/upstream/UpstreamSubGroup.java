package et.medco.etrade.upstream;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One licensed activity: an industry classification code and its description. */
public record UpstreamSubGroup(
        @JsonProperty("Code") Integer code,
        @JsonProperty("Description") String description) {
}
