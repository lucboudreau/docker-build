package org.pentaho.build.buddy.bundles.analyzer.jacoco;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bryan on 3/9/16.
 */
public class JacocoOutput {
    public static final String COVERED = "Covered";
    public static final String MISSED = "Missed";
    private final Map<String, Map<String, Map<String, Double>>> results;

    public JacocoOutput(List<File> files) throws IOException {
        results = new HashMap<>();
        for (File file : files) {
            addFile(file);
        }
    }

    public void addFile(File file) throws IOException {
        try (FileReader fileReader = new FileReader(file)) {
            for (CSVRecord csvRecord : CSVFormat.DEFAULT.withHeader().parse(fileReader)) {
                Map<String, String> rowResult = new HashMap<>();
                for (Map.Entry<String, String> columnValue : csvRecord.toMap().entrySet()) {
                    String column = columnValue.getKey().toLowerCase();
                    String[] splitColumn = column.split("_");
                    if (splitColumn.length > 1) {
                        StringBuilder columnNameBuilder = new StringBuilder(splitColumn[0]);
                        for (int i = 1; i < splitColumn.length; i++) {
                            String columnWord = splitColumn[i];
                            columnNameBuilder.append(columnWord.substring(0, 1).toUpperCase());
                            if (columnWord.length() > 1) {
                                columnNameBuilder.append(columnWord.substring(1));
                            }
                        }
                        column = columnNameBuilder.toString();
                    }
                    String value = columnValue.getValue();
                    rowResult.put(column, value);
                }
                addResult(csvRecord.get("GROUP"), csvRecord.get("PACKAGE") + "." + csvRecord.get("CLASS"), rowResult);
            }
        }
    }

    private void addResult(String groupName, String classname, Map<String, String> result) {
        Map<String, Map<String, Double>> group = results.get(groupName);
        if (group == null) {
            group = new HashMap<>();
            results.put(groupName, group);
        }
        Map<String, Double> clazz = group.get(classname);
        if (clazz == null) {
            clazz = new HashMap<>();
            group.put(classname, clazz);
        }
        clazz.putAll(coveragePercent(result));
    }

    private Map<String, Double> coveragePercent(Map<String, String> jacocoProps) {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, String> stringStringEntry : jacocoProps.entrySet()) {
            String key = stringStringEntry.getKey();
            if (key.endsWith(COVERED)) {
                String keyPrefix = key.substring(0, key.length() - COVERED.length());
                String missedKey = keyPrefix + MISSED;
                int coveredVal = Integer.parseInt(stringStringEntry.getValue());
                int missedVal = Integer.parseInt(jacocoProps.get(missedKey));
                String resultKey = "";
                if (keyPrefix.length() > 0) {
                    resultKey += keyPrefix.substring(0, 1).toUpperCase();
                    if (keyPrefix.length() > 1) {
                        resultKey += keyPrefix.substring(1).toLowerCase();
                    }
                }
                result.put(resultKey, (((double) coveredVal) / (coveredVal + missedVal)) * 100);
            }
        }
        return result;
    }

    public Map<String, Map<String, Map<String, Double>>> getResults() {
        Map<String, Map<String, Map<String, Double>>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Double>>> stringMapEntry : results.entrySet()) {
            Map<String, Map<String, Double>> nestedCopy = new HashMap<>();
            for (Map.Entry<String, Map<String, Double>> mapEntry : stringMapEntry.getValue().entrySet()) {
                nestedCopy.put(mapEntry.getKey(), new HashMap<>(mapEntry.getValue()));
            }
            result.put(stringMapEntry.getKey(), nestedCopy);
        }
        return result;
    }
}
