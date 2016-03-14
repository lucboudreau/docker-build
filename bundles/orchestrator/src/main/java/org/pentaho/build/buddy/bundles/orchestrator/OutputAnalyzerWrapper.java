package org.pentaho.build.buddy.bundles.orchestrator;

import org.pentaho.build.buddy.bundles.api.output.OutputAnalyzer;

/**
 * Created by bryan on 3/10/16.
 */
public class OutputAnalyzerWrapper implements Comparable<OutputAnalyzerWrapper> {
    private final OutputAnalyzer outputAnalyzer;
    private final int rank;

    public OutputAnalyzerWrapper(OutputAnalyzer outputAnalyzer, int rank) {
        this.outputAnalyzer = outputAnalyzer;
        this.rank = rank;
    }

    @Override
    public int compareTo(OutputAnalyzerWrapper o) {
        if (o == null) {
            return 1;
        }
        int rankDiff = o.rank - rank;
        if (rankDiff != 0) {
            return rankDiff;
        }
        return outputAnalyzer.toString().compareTo(o.outputAnalyzer.toString());
    }

    public OutputAnalyzer getOutputAnalyzer() {
        return outputAnalyzer;
    }
}
