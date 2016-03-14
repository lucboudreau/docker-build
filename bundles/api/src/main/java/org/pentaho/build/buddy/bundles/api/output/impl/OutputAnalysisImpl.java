package org.pentaho.build.buddy.bundles.api.output.impl;

import org.pentaho.build.buddy.bundles.api.output.OutputAnalysis;
import org.pentaho.build.buddy.bundles.api.output.OutputSeverity;

/**
 * Created by bryan on 3/7/16.
 */
public class OutputAnalysisImpl implements OutputAnalysis {
    private final OutputSeverity outputSeverity;
    private final String report;

    public OutputAnalysisImpl(OutputSeverity outputSeverity, String report) {
        this.outputSeverity = outputSeverity;
        this.report = report;
    }

    @Override
    public OutputSeverity getOutputSeverity() {
        return outputSeverity;
    }

    @Override
    public String getReport() {
        return report;
    }
}
