package ro.pedaleaza.cyclemap.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import ro.pedaleaza.cyclemap.data.ConfigDO;
import ro.pedaleaza.cyclemap.data.CyclePathDO;
import ro.pedaleaza.cyclemap.data.JSONDataTableExport;
import ro.pedaleaza.cyclemap.toolkit.ExcelTools;
import ro.pedaleaza.cyclemap.toolkit.KmlTools;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
            ConfigDO config = readConfig(new File("./config.yml"));

            File inputKMLFile = new File("./input/" + config.getInputFileName());
            Kml inputKml = Kml.unmarshal(inputKMLFile);
            Kml resultKml = KmlFactory.createKml();

            // init toolkits
            ExcelTools excelToolkit = new ExcelTools(new File("./input/" + config.getEvaluationFileName()));
            KmlTools kmlToolkit = new KmlTools();

            // read evaluation from excel
            Map<String, Double> criteriaMap = excelToolkit.getCriteriaAndWeightsMap(config.getEvaluationCategories());
            List<CyclePathDO> paths = excelToolkit.getLaneEvaluations(config.getEvaluationItemIndexes(), config.getEvaluationCategories());

            // set name and description
            Document resultDoc = kmlToolkit.setNameAndDescription(inputKml, resultKml);

            // set styles
            kmlToolkit.addAllStyles(resultDoc, config.getStyles());

            // create folders
            List<Placemark> placemarks = kmlToolkit.readAllPlacemarks(inputKml);
            kmlToolkit.addFolders(placemarks, resultDoc, paths, config.getFolders(), criteriaMap, config.getHost());

            Marshaller marshaller = createCleanMarshaller();
            marshaller.marshal(resultKml, new File("./output/" + config.getOutputFileNameKML()));

            // export evaluations to JSON
            ObjectWriter jsonWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
            jsonWriter.writeValue(new File("./output/" + config.getOutputFileNameJSON()), paths);

            // export to JSON for data table
            JSONDataTableExport export = new JSONDataTableExport();
            List<List<String>> data = paths.stream()
                    .map(path -> buildDatatableItem(path, config.getHost()))
                    .collect(Collectors.toList());
            export.setData(data);
            jsonWriter.writeValue(new File("./output/" + config.getOutputFileNameJSONDataTable()), export);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> buildDatatableItem(CyclePathDO path, String hostURL) {

        // this is a HACK that works because current categories are expected in alphabetical order
        List<String> categories = new ArrayList<>(path.getEvaluationByCategories().keySet());
        categories.sort(String::compareTo);

        List<String> result = new ArrayList<>();
        String href = hostURL + "detalii-pista/?laneId=" + path.getNumber() + "-" + path.getDirection();
        result.add("<a href='" + href + "' >" + getFormattedNumer(path) + "-" + path.getDirection() + "</a>");
        result.add(path.getFrom());
        result.add(path.getTo());
        result.add(path.getVia());
        result.add("" + path.getDistanceKM());
        for (String category : categories) {
            result.add("" + path.getEvaluationByCategories().get(category));
        }
        result.add("" + path.getEvaluationTotal());
        return result;
    }

    private static String getFormattedNumer(CyclePathDO path) {
        return path.getNumber() < 10 ? "0" + path.getNumber() : "" + path.getNumber();
    }

    private static ConfigDO readConfig(File file) throws IOException {
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
        return mapper.readValue(file, ConfigDO.class);
    }

    private static Marshaller createCleanMarshaller() throws Exception {
        Marshaller marshaller = JAXBContext.newInstance(Kml.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapper() {
            @Override
            public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
                return namespaceUri.matches("http://www.w3.org/\\d{4}/Atom") ? "atom"
                        : (
                        namespaceUri.matches("urn:oasis:names:tc:ciq:xsdschema:xAL:.*?") ? "xal"
                                : (
                                namespaceUri.matches("http://www.google.com/kml/ext/.*?") ? "gx"
                                        : (
                                        namespaceUri.matches("http://www.opengis.net/kml/.*?") ? ""
                                                : (
                                                null
                                        )
                                )
                        )
                );
            }
        });

        return marshaller;
    }
}
