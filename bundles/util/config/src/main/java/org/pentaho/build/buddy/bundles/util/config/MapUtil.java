package org.pentaho.build.buddy.bundles.util.config;

import java.io.IOException;
import java.util.Map;

/**
 * Created by bryan on 2/26/16.
 */
public class MapUtil {
    public static String getStringOrThrow(Map config, String field) throws IOException {
        Object o = config.get(field);
        if (o == null) {
            throw new IOException(field + " cannot be null");
        }
        return o.toString();
    }

    public static String getStringOrNull(Map config, String field) throws IOException {
        Object o = config.get(field);
        if (o == null) {
            return null;
        }
        return o.toString();
    }

    public static boolean getValue(Map config, String field, boolean defaultVal) throws IOException {
        String stringOrNull = getStringOrNull(config, field);
        if (stringOrNull == null) {
            return defaultVal;
        }
        return Boolean.parseBoolean(stringOrNull);
    }
}
