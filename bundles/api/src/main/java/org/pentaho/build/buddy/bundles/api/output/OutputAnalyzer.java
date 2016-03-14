package org.pentaho.build.buddy.bundles.api.output;

import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;

import java.io.IOException;

/**
 * Created by bryan on 2/16/16.
 */
public interface OutputAnalyzer {
    OutputAnalysis analyzeOutput(SourceRetrievalResult sourceRetrievalResult, LineHandler stdoutHandler, LineHandler stderrLineHandler) throws IOException;
}
