package ro.pedaleaza.cyclemap.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ConfigDO {
    @JsonProperty
    private String inputFileName;

    @JsonProperty
    private String outputFileNameKML;

    @JsonProperty
    private String outputFileNameJSON;

    @JsonProperty
    private String outputFileNameJSONDataTable;

    @JsonProperty
    private String evaluationFileName;

    @JsonProperty
    private String host;

    @JsonProperty
    private List<StyleDO> styles;

    @JsonProperty
    private List<String> evaluationItemIndexes;

    @JsonProperty
    private List<FolderDO> folders;

    @JsonProperty
    private Map<String, String> evaluationCategories;
}
