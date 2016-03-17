package org.pentaho.build.buddy.bundles.analyzer.buildteam;

import org.pentaho.build.buddy.bundles.api.output.OutputAnalysis;
import org.pentaho.build.buddy.bundles.api.output.OutputAnalyzer;
import org.pentaho.build.buddy.bundles.api.output.OutputSeverity;
import org.pentaho.build.buddy.bundles.api.output.impl.OutputAnalysisImpl;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.util.template.FTLUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by bryan on 3/17/16.
 */
public class BuildTeamImpactAnalyzer implements OutputAnalyzer {
    private final List<Pattern> sensitivePatterns;
    private final FTLUtil ftlUtil;

    public BuildTeamImpactAnalyzer(String sensitiveFiles) {
        this.sensitivePatterns = new ArrayList<>();
        for (String pattern : sensitiveFiles.split(",")) {
            sensitivePatterns.add(Pattern.compile(pattern));
        }
        this.ftlUtil = new FTLUtil(BuildTeamImpactAnalyzer.class);
    }

    @Override
    public OutputAnalysis analyzeOutput(SourceRetrievalResult sourceRetrievalResult, LineHandler stdoutHandler, LineHandler stderrLineHandler) throws IOException {
        List<String> sensitiveFiles = new ArrayList<>();
        for (String changedFile : sourceRetrievalResult.getChangedFiles()) {
            String file = new File(sourceRetrievalResult.getHeadDir(), changedFile).getName();
            for (String sensitiveFile : sensitiveFiles) {
                if (file.endsWith(sensitiveFile)) {
                    sensitiveFiles.add(file);
                }
            }
        }
        return new OutputAnalysisImpl(OutputSeverity.INFO, ftlUtil.render("buildteam.ftl", "sensitiveFiles", sensitiveFiles));
    }

    @Override
    public String getDescription() {
        return "Check for build file changes";
    }

    @Override
    public String getId() {
        return "BuildTeam";
    }
}
