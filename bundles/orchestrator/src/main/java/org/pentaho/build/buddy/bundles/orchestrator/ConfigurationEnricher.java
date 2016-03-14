package org.pentaho.build.buddy.bundles.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by bryan on 3/11/16.
 */
public class ConfigurationEnricher {
    private final Map<String, List<List<Map<String, Object>>>> enrichSpec;

    public ConfigurationEnricher() throws IOException {
        this(getEnrichSpec());
    }

    public ConfigurationEnricher(Map<String, List<List<Map<String, Object>>>> enrichSpec) {
        this.enrichSpec = enrichSpec;
    }

    private static Map getEnrichSpec() throws IOException {
        return new ObjectMapper().readValue(new File(System.getProperty("karaf.etc"), "wingman.json"), Map.class);
    }

    public Map enrich(Map<String, Object> config, String phase) {
        Map<String, Object> result = deepCopy(config);
        List<List<Map<String, Object>>> operations = enrichSpec.get(phase);
        for (List<Map<String, Object>> operation : operations) {
            if (matches(result, operation.get(0))) {
                apply(result, operation.get(1), result);
            }
        }
        return result;
    }

    private void apply(Map<String, Object> config, Map<String, Object> operation, Map<String, Object> topLevel) {
        for (Map.Entry<String, Object> stringObjectEntry : operation.entrySet()) {
            String key = stringObjectEntry.getKey();
            Object value = stringObjectEntry.getValue();
            Object existing = config.get(key);
            if (existing instanceof Map && value instanceof Map) {
                apply((Map<String, Object>) existing, (Map<String, Object>) value, topLevel);
            } else if (value instanceof String) {
                if (((String) value).startsWith("${") && ((String) value).endsWith("}")) {
                    config.put(key, System.getenv(((String) value).substring(2, ((String) value).length() - 1)));
                } else if (((String) value).startsWith("#{") && ((String) value).endsWith("}")) {
                    String[] path = ((String) value).substring(2, ((String) value).length() - 1).split(Pattern.quote("."));
                    Map drillDown = topLevel;
                    for (int i = 0; i < path.length - 1; i++) {
                        drillDown = (Map)drillDown.get(path[i]);
                    }
                    config.put(key, drillDown.get(path[path.length - 1]));
                } else {
                    config.put(key, value);
                }
            } else {
                config.put(key, deepCopy(value));
            }
        }
    }

    private Object deepCopy(Object object) {
        if (object instanceof Map) {
            return deepCopy((Map<String, Object>) object);
        } else if (object instanceof Collection) {
            return deepCopy((Collection<Object>) object);
        } else {
            return object;
        }
    }

    private List<Object> deepCopy(Collection<Object> objects) {
        List<Object> dest = new ArrayList<>();
        for (Object o : objects) {
            dest.add(deepCopy(o));
        }
        return dest;
    }

    private Map deepCopy(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> stringEntry : source.entrySet()) {
            result.put(stringEntry.getKey(), deepCopy(stringEntry.getValue()));
        }
        return result;
    }

    private boolean matches(Map<String, ?> confg, Map<String, ?> condition) {
        for (Map.Entry<String, ?> entry : condition.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object configValue = confg.get(key);
            if (value == null) {
                if (configValue != null) {
                    return false;
                }
            } else if (value instanceof Map && configValue instanceof Map) {
                if (!matches((Map) configValue, (Map) value)) {
                    return false;
                }
            } else if (!value.equals(configValue)) {
                return false;
            }
        }
        return true;
    }
}
