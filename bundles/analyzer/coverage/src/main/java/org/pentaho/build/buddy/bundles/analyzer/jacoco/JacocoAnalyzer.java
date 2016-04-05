package org.pentaho.build.buddy.bundles.analyzer.jacoco;

import org.pentaho.build.buddy.bundles.api.output.OutputAnalysis;
import org.pentaho.build.buddy.bundles.api.output.OutputAnalyzer;
import org.pentaho.build.buddy.bundles.api.output.OutputSeverity;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.util.template.FTLUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by bryan on 3/9/16.
 */
public class JacocoAnalyzer implements OutputAnalyzer {
    private final FTLUtil ftlUtil;
    private final Set<Pattern> jacocoUnitPatterns;
    private final Set<Pattern> jacocoIntegrationPatterns;

    public JacocoAnalyzer(String jacocoUnitPatterns, String jacocoIntegrationPatterns) {
        ftlUtil = new FTLUtil(JacocoAnalyzer.class);
        this.jacocoUnitPatterns = new HashSet<>();
        for (String pattern : jacocoUnitPatterns.split(",")) {
            this.jacocoUnitPatterns.add(Pattern.compile(pattern.trim()));
        }
        this.jacocoIntegrationPatterns = new HashSet<>();
        for (String pattern : jacocoIntegrationPatterns.split(",")) {
            this.jacocoIntegrationPatterns.add(Pattern.compile(pattern.trim()));
        }
    }

    @Override
    public OutputAnalysis analyzeOutput(SourceRetrievalResult sourceRetrievalResult, LineHandler stdoutHandler, LineHandler stderrLineHandler) throws IOException {
        JacocoOutput baseJacocoUnitOutput = new JacocoOutput(findFiles(sourceRetrievalResult.getBaseDir(), jacocoUnitPatterns));
        JacocoOutput headJacocoUnitOutput = new JacocoOutput(findFiles(sourceRetrievalResult.getHeadDir(), jacocoUnitPatterns));
        JacocoOutput baseJacocoIntegrationOutput = new JacocoOutput(findFiles(sourceRetrievalResult.getBaseDir(), jacocoIntegrationPatterns));
        JacocoOutput headJacocoIntegrationOutput = new JacocoOutput(findFiles(sourceRetrievalResult.getHeadDir(), jacocoIntegrationPatterns));

        JacocoDiff jacocoUnitDiff = new JacocoDiff(baseJacocoUnitOutput, headJacocoUnitOutput);
        JacocoDiff jacocoIntegrationDiff = new JacocoDiff(baseJacocoIntegrationOutput, headJacocoIntegrationOutput);

        Map unitData = new HashMap();
        unitData.put("header", "Unit test coverage change");
        unitData.put("results", jacocoUnitDiff.getResults());
        String unitMarkdown = ftlUtil.render("jacoco.ftl", unitData);

        Map integrationData = new HashMap();
        integrationData.put("header", "Integration test coverage change");
        integrationData.put("results", jacocoIntegrationDiff.getResults());
        String integrationMarkdown = ftlUtil.render("jacoco.ftl", integrationData);

        return new OutputAnalysis.Builder()
            .severity(OutputSeverity.max(Arrays.asList(jacocoUnitDiff.sev, jacocoIntegrationDiff.sev)))
            .report(unitMarkdown + "\n\n" + integrationMarkdown)
            .build();
    }

    @Override
    public String getDescription() {
        return "Test coverage";
    }

    private List<File> findFiles(File root, final Set<Pattern> includePatterns) throws IOException {
        final List<File> result = new ArrayList<>();
        Files.walkFileTree(Paths.get(root.toURI()), new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                File file = path.toFile();
                String normalizedPathName = file.getAbsolutePath().replace("\\", "/");
                for (Pattern includePattern : includePatterns) {
                    if (includePattern.matcher(normalizedPathName).matches()) {
                        result.add(file);
                        break;
                    }
                }
                return super.visitFile(path, attrs);
            }
        });
        return result;
    }

    @Override
    public String getId() {
        return "Coverage";
    }
}
