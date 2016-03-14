package org.pentaho.build.buddy.bundles.analyzer.jacoco;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bryan on 3/9/16.
 */
public class JacocoDiff {
    private final Map<String, Map<String, Map<String, Double>>> results;

    public JacocoDiff(JacocoOutput before, JacocoOutput after) {
        results = new HashMap<>();
        Map<String, Map<String, Map<String, Double>>> beforeResults = before.getResults();
        if (beforeResults == null) {
            beforeResults = new HashMap<>();
        }
        Map<String, Map<String, Map<String, Double>>> afterResults = after.getResults();
        if (afterResults == null) {
            afterResults = new HashMap<>();
        }
        for (Map.Entry<String, Map<String, Map<String, Double>>> stringMapEntry : beforeResults.entrySet()) {
            Map<String, Map<String, Double>> afterGroup = afterResults.remove(stringMapEntry.getKey());
            if (afterGroup == null) {
                afterGroup = new HashMap<>();
            }
            this.results.put(stringMapEntry.getKey(), diffGroup(stringMapEntry.getValue(), afterGroup));
        }
        for (Map.Entry<String, Map<String, Map<String, Double>>> stringMapEntry : afterResults.entrySet()) {
            this.results.put(stringMapEntry.getKey(), diffGroup(new HashMap<String, Map<String, Double>>(), stringMapEntry.getValue()));
        }
    }

    private Map<String, Map<String, Double>> diffGroup(Map<String, Map<String, Double>> before, Map<String, Map<String, Double>> after) {
        Map<String, Map<String, Double>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> stringMapEntry : before.entrySet()) {
            Map<String, Double> afterClass = after.remove(stringMapEntry.getKey());
            if (afterClass == null) {
                afterClass = new HashMap<>();
            }
            result.put(stringMapEntry.getKey(), diffClass(stringMapEntry.getValue(), afterClass));
        }
        for (Map.Entry<String, Map<String, Double>> stringMapEntry : after.entrySet()) {
            result.put(stringMapEntry.getKey(), diffClass(new HashMap<String, Double>(), stringMapEntry.getValue()));
        }
        return result;
    }

    private Map<String, Double> diffClass(Map<String, Double> before, Map<String, Double> after) {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Double> stringDoubleEntry : before.entrySet()) {
            Double afterValue = after.remove(stringDoubleEntry.getKey());
            if (afterValue == null) {
                afterValue = Double.valueOf(0);
            }
            double change = afterValue - stringDoubleEntry.getValue();
            if (Math.abs(change) > 0.0001) {
                result.put(stringDoubleEntry.getKey() + " Change", change);
            }
        }
        for (Map.Entry<String, Double> stringDoubleEntry : after.entrySet()) {
            Double newVal = stringDoubleEntry.getValue();
            if (newVal > 0.0001) {
                result.put(stringDoubleEntry.getKey() + " New Coverage", newVal);
            }
        }
        return result;
    }

    public Map<String, Map<String, Map<String, Double>>> getResults() {
        Map<String, Map<String, Map<String, Double>>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Double>>> stringMapEntry : results.entrySet()) {
            Map<String, Map<String, Double>> value = stringMapEntry.getValue();
            Map<String, Map<String, Double>> copy = new HashMap<>();
            for (Map.Entry<String, Map<String, Double>> mapEntry : value.entrySet()) {
                Map<String, Double> mapEntryValue = mapEntry.getValue();
                if (mapEntryValue.size() > 0) {
                    copy.put(mapEntry.getKey(), new HashMap<>(mapEntryValue));
                }
            }
            if (copy.size() > 0) {
                result.put(stringMapEntry.getKey(), copy);
            }
        }
        return result;
    }
}
