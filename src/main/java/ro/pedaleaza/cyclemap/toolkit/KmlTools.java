package ro.pedaleaza.cyclemap.toolkit;

import de.micromata.opengis.kml.v_2_2_0.*;
import ro.pedaleaza.cyclemap.data.CyclePathDO;
import ro.pedaleaza.cyclemap.data.FolderDO;
import ro.pedaleaza.cyclemap.data.StyleDO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KmlTools {
    public Document setNameAndDescription(Kml inputKml, Kml resultKml) {
        Document doc = (Document) inputKml.getFeature();

        Document resultDoc = resultKml.createAndSetDocument();
        resultDoc.setName(doc.getName());
        resultDoc.setDescription(doc.getDescription());

        return resultDoc;
    }

    public void addAllStyles(Document resultDoc, List<StyleDO> styles) {
        for (StyleDO style : styles) {
            // create styles
            Style normalStyle = addStyle(resultDoc, style, false);
            Style highlightStyle = addStyle(resultDoc, style, true);

            // create style maps
            StyleMap styleMap = resultDoc.createAndAddStyleMap();
            styleMap.setId(style.getName());

            Pair normalPair = styleMap.createAndAddPair();
            normalPair.setKey(StyleState.NORMAL);
            normalPair.setStyleUrl("#" + normalStyle.getId());

            Pair highlightPair = styleMap.createAndAddPair();
            highlightPair.setKey(StyleState.HIGHLIGHT);
            highlightPair.setStyleUrl("#" + highlightStyle.getId());
        }
    }

    private Style addStyle(Document resultDoc, StyleDO style, boolean highlight) {
        Style styleToAdd = resultDoc.createAndAddStyle();
        styleToAdd.setId(style.getName() + (highlight ? "-highlight" : "-normal"));

        BalloonStyle balloonStyle = styleToAdd.createAndSetBalloonStyle();
        balloonStyle.setText("<![CDATA[<h3>$[name]</h3>]]>");

        LineStyle lineStyle = styleToAdd.createAndSetLineStyle();
        lineStyle.setColor(style.getColor());
        lineStyle.setWidth(style.getWidth() + (highlight ? 1 : 0));

        return styleToAdd;
    }

    public void addFolders(List<Placemark> inputPlacemarks, Document resultDoc, List<CyclePathDO> evaluation, List<FolderDO> folders, Map<String, Double> criteriaMap, String host) {
        List<Placemark> unusedPlacemarks = new ArrayList<>(inputPlacemarks);
        double totalKm = 0;

        for (FolderDO folderDef : folders) {
            Folder folder = resultDoc.createAndAddFolder();
            folder.setName(folderDef.getName());
            folder.setStyleUrl(folderDef.getStyle());

            // match all paths evaluated for this folder
            for (CyclePathDO path : evaluation) {
                double score = path.getEvaluationTotal();

                if (score >= folderDef.getMinimumScore() && score < folderDef.getMaximumScore()) {
                    double km = 0;

                    for (String segment : path.getSegments()) {
                        // find corresponding Placemark
                        Placemark placemark = findPlacemark(path.getNumber(), segment, inputPlacemarks);
                        if (placemark == null) {
                            System.out.println("Cannot find placemark for " + path.getNumber() + ", seg: " + segment);
                            continue;
                        }
                        unusedPlacemarks.remove(placemark);

                        Placemark resultPlacemark = folder.createAndAddPlacemark();
                        resultPlacemark.setName(placemark.getName());
                        resultPlacemark.setDescription(buildPlacemarkDescription(path, criteriaMap, host));
                        resultPlacemark.setStyleUrl("#" + folderDef.getStyle());
                        LineString line = resultPlacemark.createAndSetLineString();
                        line.setTessellate(true);

                        List<Coordinate> coordinates = ((LineString) placemark.getGeometry()).getCoordinates();
                        line.setCoordinates(coordinates);

                        for (int i = 0; i < coordinates.size() - 1; i++) {
                            Coordinate current = coordinates.get(i);
                            Coordinate next = coordinates.get(i + 1);
                            km += distanceInKmBetweenEarthCoordinates(
                                    current.getLatitude(),
                                    current.getLongitude(),
                                    next.getLatitude(),
                                    next.getLongitude());
                        }
                        // trim it down to two decimals
                        int temp = (int) (km * 100.0);
                        km = ((double) temp) / 100.0;
                    }

                    totalKm += km;
                    path.setDistanceKM(km);
                }
            }
        }

        System.out.println("Total km: " + totalKm);

        if (!unusedPlacemarks.isEmpty()) {
            System.out.println("Placemarks not used:");
            for (Placemark placemark : unusedPlacemarks)
                System.out.println(placemark.getName());
        }
    }

    public List<Placemark> readAllPlacemarks(Kml inputKml) {
        List<Placemark> result = new ArrayList<>();
        Document doc = (Document) inputKml.getFeature();
        for (Feature feature : doc.getFeature()) {
            for (Feature mark : ((Folder) feature).getFeature())
                result.add((Placemark) mark);
        }

        return result;
    }

    private String buildPlacemarkDescription(CyclePathDO path, Map<String, Double> criteriaMap, String host) {
        String result = "Punctaj total: " + path.getEvaluationTotal() + "<br><br>";

        result += String.join(
                "<br>",
                path.getEvaluationByCategories().keySet()
                        .stream()
                        .map(key -> "" + key + ": " + path.getEvaluationByCategories().get(key) + " / " + criteriaMap.get(key))
                        .collect(Collectors.toList()));

        result += "<br>" + host + "detalii-pista/?laneId=" + path.getNumber() + "-" + path.getDirection();

        return result;
    }

    private Placemark findPlacemark(int pathNumber, String segment, List<Placemark> inputPlacemarks) {
        List<String> namesList = new ArrayList<>();
        namesList.add("Pista " + pathNumber + " - segment " + segment);

        for (Placemark placemark : inputPlacemarks)
            if (namesList.contains(placemark.getName()))
                return placemark;

        return null;
    }

    private double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    private double distanceInKmBetweenEarthCoordinates(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371;

        double dLat = degreesToRadians(lat2 - lat1);
        double dLon = degreesToRadians(lon2 - lon1);

        lat1 = degreesToRadians(lat1);
        lat2 = degreesToRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
