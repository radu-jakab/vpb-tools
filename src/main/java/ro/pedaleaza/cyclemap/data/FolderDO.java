package ro.pedaleaza.cyclemap.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class FolderDO {
    @JsonProperty
    private String name;

    @JsonProperty
    private String style;

    @JsonProperty
    private int minimumScore;

    @JsonProperty
    private int maximumScore;
}
