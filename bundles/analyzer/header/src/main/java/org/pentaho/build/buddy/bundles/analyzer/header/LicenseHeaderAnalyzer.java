package org.pentaho.build.buddy.bundles.analyzer.header;

import org.pentaho.build.buddy.bundles.api.output.OutputAnalysis;
import org.pentaho.build.buddy.bundles.api.output.OutputAnalyzer;
import org.pentaho.build.buddy.bundles.api.output.OutputSeverity;
import org.pentaho.build.buddy.bundles.api.output.impl.OutputAnalysisImpl;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.util.template.FTLUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bryan on 3/7/16.
 */
public class LicenseHeaderAnalyzer implements OutputAnalyzer {
    private final FTLUtil ftlUtil;
    private final String[] validLicenses;

    public LicenseHeaderAnalyzer() {
        ftlUtil = new FTLUtil(LicenseHeaderAnalyzer.class);
        validLicenses = new String[]{"Licensed under the Apache License", "GNU Lesser General Public License", "GNU General Public License", "PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL"};
    }

    @Override
    public OutputAnalysis analyzeOutput(SourceRetrievalResult sourceRetrievalResult, LineHandler stdoutHandler, LineHandler stderrLineHandler) throws IOException {
        List<String> violations = new ArrayList<>();
        for (String changedFile : sourceRetrievalResult.getChangedFiles()) {
            final File file = new File(sourceRetrievalResult.getHeadDir(), changedFile);
            if (file.getName().endsWith(".java")) {
                analyzeFile(violations, changedFile, file, stderrLineHandler);
            }
        }
        String violationsText = ftlUtil.render("license.ftl", "violations", violations);
        return new OutputAnalysisImpl(violations.size() == 0 ? OutputSeverity.INFO : OutputSeverity.ERROR, violationsText);
    }

    @Override
    public String getDescription() {
        return "License header check";
    }

    void analyzeFile(List<String> violations, String fileName, File file, LineHandler stderrLineHandler) throws IOException {
        boolean foundCopyright = false;
        boolean foundLicense = false;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((!foundCopyright || !foundLicense) && ((line = bufferedReader.readLine()) != null)) {
                if (!foundCopyright && line.toLowerCase().contains("copyright")) {
                    foundCopyright = true;
                }
                if (!foundLicense) {
                    for (String validLicense : validLicenses) {
                        if (line.contains(validLicense)) {
                            foundLicense = true;
                            break;
                        }
                    }
                }
            }
        }
        if (!foundCopyright) {
            if (!foundLicense) {
                stderrLineHandler.handle("Couldn't find acceptable license or copyright on " + fileName);
            } else {
                stderrLineHandler.handle("Couldn't find acceptable copyright on " + fileName);
            }
            violations.add(fileName);
        } else if (!foundLicense) {
            stderrLineHandler.handle("Couldn't find acceptable license on " + fileName);
            violations.add(fileName);
        }
    }

    @Override
    public String getId() {
        return "License";
    }
}
