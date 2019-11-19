package ro.pedaleaza.cyclemap.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JSONDataTableExport {
    private List<List<String>> data;
}
