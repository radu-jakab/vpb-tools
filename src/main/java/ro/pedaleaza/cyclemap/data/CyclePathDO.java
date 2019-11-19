package ro.pedaleaza.cyclemap.data;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CyclePathDO {

    private int number;

    private String direction;

    private List<String> segments;

    private Map<String, Double> evaluation;

    private Map<String, Double> evaluationPercentage;

    private Map<String, Double> evaluationByCategories;

    private String remarks = "";

    private String from = "";

    private String to = "";

    private String via = "";

    private double distanceKM;

    private String movieLink;

    public double getEvaluationTotal() {
        double sum = 0;
        for (Double value : evaluationByCategories.values())
            sum += value;
        return sum;
    }
}
