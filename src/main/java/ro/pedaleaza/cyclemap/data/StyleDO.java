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
public class StyleDO {

    @JsonProperty
    private String name;

    @JsonProperty
    private String color;

    @JsonProperty
    private double width;
}
