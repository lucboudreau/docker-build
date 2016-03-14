package org.pentaho.build.buddy.bundles.analyzer.changed;

import org.pentaho.build.buddy.bundles.api.output.OutputAnalysis;
import org.pentaho.build.buddy.bundles.api.output.OutputAnalyzer;
import org.pentaho.build.buddy.bundles.api.output.OutputSeverity;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.util.template.FTLUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by bryan on 3/7/16.
 */
public class ChangedAnalyzer implements OutputAnalyzer {
    private final FTLUtil ftlUtil;

    public ChangedAnalyzer() {
        ftlUtil = new FTLUtil(ChangedAnalyzer.class);
    }

    @Override
    public OutputAnalysis analyzeOutput(SourceRetrievalResult sourceRetrievalResult, LineHandler stdoutHandler, LineHandler stderrLineHandler) throws IOException {
        List<String> files = new ArrayList<>(sourceRetrievalResult.getChangedFiles());
        Collections.sort(files);
        final String render = ftlUtil.render("changed.ftl", "fileList", files);
        return new OutputAnalysis() {
            @Override
            public OutputSeverity getOutputSeverity() {
                return OutputSeverity.INFO;
            }

            @Override
            public String getReport() {
                return render;
            }
        };
    }
}
