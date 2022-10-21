package ro.pedaleaza.cyclemap.toolkit;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ro.pedaleaza.cyclemap.data.CyclePathDO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelTools {
    private XSSFSheet sheet;

    public ExcelTools(File workbookFile) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(workbookFile.toPath()));
        sheet = workbook.getSheet("Lista si Evaluare");
        if (sheet == null)
            sheet = workbook.getSheetAt(0);
    }

    public Map<String, Double> getCriteriaAndWeightsMap(Map<String, String> indexes) {
        Map<String, Double> result = new HashMap<>();

        for (String index : indexes.keySet()) {
            char startCol = index.charAt(0);
            int row = Integer.parseInt(index.substring(1));

            String categoryName = getCell(row, startCol).getStringCellValue();

            double weight = 0;
            for (char col = startCol; col <= indexes.get(index).charAt(3); col++)
                weight += getCell(row + 2, col).getNumericCellValue();
            System.out.println("Read criterion: " + categoryName + ", weight: " + weight);

            result.put(categoryName, weight);
        }

        return result;
    }

    public List<CyclePathDO> getLaneEvaluations(List<String> indexes, Map<String, String> evaluationCategories) {
        List<CyclePathDO> result = new ArrayList<>();

        for (int i = 6; ; i++) {
            int numberRowNr = (i % 2 == 0) ? i : i - 1;
            int laneNr = (int) getCell(numberRowNr, 'b').getNumericCellValue();
            String from = getCell(i, 'c').getStringCellValue();
            String segments = getCell(i, 'f').getStringCellValue();

            if (laneNr == 0)
                break;
            if (from.trim().isEmpty() || segments.trim().isEmpty()) {
//                System.out.println("Reading evaluations, skipping line " + i);
                continue;
            }

//            System.out.println("Reading line " + i);

            CyclePathDO path = new CyclePathDO();
            path.setNumber(laneNr);
            path.setSegments(splitSegments(segments.substring(1))); // skip prefix 's'
            path.setFrom(getCell(i, 'c').getStringCellValue());
            path.setTo(getCell(i, 'd').getStringCellValue());
            path.setVia(getCell(numberRowNr, 'e').getStringCellValue());
            path.setDirection((i % 2 == 0) ? "dus" : "intors");

            String youtubeId = getCell(i, 35).getStringCellValue();
            if (!youtubeId.trim().isEmpty())
                path.setMovieLink("https://www.youtube.com/embed/" + youtubeId);
            else
                path.setMovieLink("");

            // add evaluations
            Map<String, Double> evaluations = new HashMap<>();
            Map<String, Double> evaluationPercentages = new HashMap<>();
            for (String index : indexes) {
                char col = index.charAt(0);
                int row = Integer.parseInt(index.substring(1));

                String criterionName = getCell(row, col).getStringCellValue();
                Double criterionWeight = getCell(row + 1, col).getNumericCellValue();
                Double evalValue = getCell(i, col).getNumericCellValue();

                evaluations.put(criterionName, criterionWeight * evalValue);
                evaluationPercentages.put(criterionName, evalValue);
            }
            path.setEvaluation(evaluations);
            path.setEvaluationPercentage(evaluationPercentages);

            // add category evaluations
            Map<String, Double> evaluationsByCategories = new HashMap<>();
            for (String category : evaluationCategories.keySet()) {
                int categoryNameLineNr = Integer.parseInt(category.substring(1));
                String name = getCell(categoryNameLineNr, category.charAt(0)).getStringCellValue();
                int line = evaluationCategories.get(category).charAt(1) - '0';
                char startCol = evaluationCategories.get(category).charAt(0);
                char endCol = evaluationCategories.get(category).charAt(3);
                double sum = 0;
                for (char col = startCol; col <= endCol; col++) {
                    String evalName = getCell(line, col).getStringCellValue();
                    sum += evaluations.get(evalName);
                }
                evaluationsByCategories.put(name, sum);
            }
            path.setEvaluationByCategories(evaluationsByCategories);

            // add evaluated path to results list
            result.add(path);
        }

        return result;
    }

    private List<String> splitSegments(String segmentString) {
        List<String> result = new ArrayList<>();

        String[] splitByComma = segmentString.split(",");
        for (String s : splitByComma) {
            if (s.contains("-")) {
                int min = Integer.parseInt(s.split("-")[0]);
                int max = Integer.parseInt(s.split("-")[1]);
                for (int i = min; i <= max; i++)
                    result.add("" + i);
            } else result.add(s);
        }

        return result;
    }

    private XSSFCell getCell(int row, int col) {
        row -= 1;
        if (sheet.getRow(row) == null)
            sheet.createRow(row);
        if (sheet.getRow(row).getCell(col) == null)
            sheet.getRow(row).createCell(col);
        return sheet.getRow(row).getCell(col);
    }

    private XSSFCell getCell(int row, char col) {
        return getCell(row, col - 'a');
    }

}
