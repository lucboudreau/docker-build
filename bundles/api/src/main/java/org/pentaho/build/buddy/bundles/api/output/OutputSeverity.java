package org.pentaho.build.buddy.bundles.api.output;

import java.util.Collection;

/**
 * Created by bryan on 2/16/16.
 */
public enum OutputSeverity {
    INFO,
    WARNING,
    ERROR;

    public static OutputSeverity max(Collection<OutputSeverity> outputSeverities) {
        if (outputSeverities.size() == 0) {
            return null;
        }
        int max = INFO.ordinal();
        for (OutputSeverity outputSeverity : outputSeverities) {
            if (outputSeverity.ordinal() > max) {
                max = outputSeverity.ordinal();
            }
        }
        return OutputSeverity.values()[max];
    }
}
