package org.pentaho.build.buddy.bundles.analyzer.tests;

import org.pentaho.build.buddy.bundles.api.output.OutputAnalysis;
import org.pentaho.build.buddy.bundles.api.output.OutputAnalyzer;
import org.pentaho.build.buddy.bundles.api.output.OutputSeverity;
import org.pentaho.build.buddy.bundles.api.output.impl.OutputAnalysisImpl;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.util.template.FTLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by bryan on 3/8/16.
 */
public class TestsAnalyzer implements OutputAnalyzer {
    private final FTLUtil ftlUtil;
    private final Set<Pattern> testPatterns;

    public TestsAnalyzer(String testPatterns) {
        this.ftlUtil = new FTLUtil(TestsAnalyzer.class);
        this.testPatterns = new HashSet<>();
        for (String pattern : testPatterns.split(",")) {
            this.testPatterns.add(Pattern.compile(pattern.trim()));
        }
    }

    @Override
    public OutputAnalysis analyzeOutput(SourceRetrievalResult sourceRetrievalResult, LineHandler stdoutHandler, LineHandler stderrLineHandler) throws IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        Map<String, TestSuite> baseSuites = parseSuites(sourceRetrievalResult.getBaseDir(), documentBuilderFactory);
        Map<String, TestSuite> headSuites = parseSuites(sourceRetrievalResult.getHeadDir(), documentBuilderFactory);
        Map<String, TestSuiteDiff> testSuiteDiffMap = new HashMap<>();
        for (Map.Entry<String, TestSuite> stringTestSuiteEntry : baseSuites.entrySet()) {
            testSuiteDiffMap.put(stringTestSuiteEntry.getKey(), new TestSuiteDiff(stringTestSuiteEntry.getValue(), headSuites.remove(stringTestSuiteEntry.getKey())));
        }
        for (Map.Entry<String, TestSuite> stringTestSuiteEntry : headSuites.entrySet()) {
            testSuiteDiffMap.put(stringTestSuiteEntry.getKey(), new TestSuiteDiff(null, stringTestSuiteEntry.getValue()));
        }

        Map<String, Map<String, TestSuite>> dataMap = new HashMap<>();
        Map<String, TestSuite> newlyBrokenTests = new HashMap<>();
        Map<String, TestSuite> newlyFixedTests = new HashMap<>();
        Map<String, TestSuite> stillBrokenTests = new HashMap<>();
        for (Map.Entry<String, TestSuiteDiff> stringTestSuiteDiffEntry : testSuiteDiffMap.entrySet()) {
            String suiteName = stringTestSuiteDiffEntry.getKey();
            TestSuiteDiff testSuiteDiff = stringTestSuiteDiffEntry.getValue();
            TestSuite newlyBroken = testSuiteDiff.getNewlyBroken();
            if (newlyBroken.hasErrors()) {
                newlyBrokenTests.put(suiteName, newlyBroken);
            }
            TestSuite newlyFixed = testSuiteDiff.getNewlyFixed();
            if (newlyFixed.hasErrors()) {
                newlyFixedTests.put(suiteName, newlyFixed);
            }
            TestSuite stillBroken = testSuiteDiff.getStillBroken();
            if (stillBroken.hasErrors()) {
                stillBrokenTests.put(suiteName, stillBroken);
            }
        }

        dataMap.put("broken", newlyBrokenTests);
        dataMap.put("fixed", newlyFixedTests);
        dataMap.put("stillBroken", stillBrokenTests);

        final OutputSeverity outputSeverity = testSuiteDiffMap.size() == 0 ? OutputSeverity.INFO : OutputSeverity.ERROR;
        return new OutputAnalysisImpl(outputSeverity, ftlUtil.render("tests.ftl", dataMap));
    }

    private Map<String, TestSuite> parseSuites(File rootDir, DocumentBuilderFactory documentBuilderFactory) throws IOException {
        Map<String, TestSuite> result = new HashMap<>();
        for (File file : getTestXmls(rootDir)) {
            Document document;
            try {
                document = documentBuilderFactory.newDocumentBuilder().parse(file);
            } catch (Exception e) {
                throw new IOException(e);
            }

            TestSuite testSuite = new TestSuite();
            testSuite.addSuiteErrors(findErrors(document.getFirstChild(), "error", "error"));

            NodeList childNodes = document.getFirstChild().getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                String nodeName = node.getNodeName();
                if ("testcase".equals(nodeName)) {
                    String className = node.getAttributes().getNamedItem("classname").getNodeValue();
                    String name = node.getAttributes().getNamedItem("name").getNodeValue();
                    for (TestError testError : findErrors(node, "error", "error")) {
                        testSuite.addCaseError(className, name, testError);
                    }
                    for (TestError testError : findErrors(node, "failure", "failure")) {
                        testSuite.addCaseError(className, name, testError);
                    }
                }
            }

            if (testSuite.hasErrors()) {
                String suiteName = document.getFirstChild().getAttributes().getNamedItem("name").getNodeValue();
                result.put(suiteName, testSuite);
            }
        }
        return result;
    }

    private List<TestError> findErrors(Node parentNode, String nodeName, String severity) {
        List<TestError> result = new ArrayList<>();
        NodeList childNodes = parentNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (nodeName.equals(node.getNodeName())) {
                result.add(new TestError(node, severity));
            }
        }
        return result;
    }

    private List<File> getTestXmls(File rootDir) throws IOException {
        final List<File> results = new ArrayList<>();
        Files.walkFileTree(Paths.get(rootDir.toURI()), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                File file = path.toFile();
                String normalizedPathName = file.getAbsolutePath().replace("\\", "/");
                for (Pattern testPattern : testPatterns) {
                    if (testPattern.matcher(normalizedPathName).matches()) {
                        results.add(file);
                    }
                }
                return super.visitFile(path, attrs);
            }
        });
        return results;
    }
}
