package org.pentaho.build.buddy.bundles.api.output;

/**
 * Created by bryan on 2/16/16.
 */
public interface OutputAnalysis {
    OutputSeverity getOutputSeverity();

    String getReport();
}
