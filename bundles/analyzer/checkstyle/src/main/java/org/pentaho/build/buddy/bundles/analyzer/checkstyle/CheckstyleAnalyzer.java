package org.pentaho.build.buddy.bundles.analyzer.checkstyle;

import org.pentaho.build.buddy.bundles.api.output.OutputAnalysis;
import org.pentaho.build.buddy.bundles.api.output.OutputAnalyzer;
import org.pentaho.build.buddy.bundles.api.output.OutputChangeDetector;
import org.pentaho.build.buddy.bundles.api.output.OutputSeverity;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.util.template.FTLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by bryan on 3/7/16.
 */
public class CheckstyleAnalyzer implements OutputAnalyzer {
    private final FTLUtil ftlUtil;
    private final List<OutputChangeDetector> outputChangeDetectors;

    public CheckstyleAnalyzer(List<OutputChangeDetector> outputChangeDetectors) {
        this.outputChangeDetectors = outputChangeDetectors;
        this.ftlUtil = new FTLUtil(CheckstyleAnalyzer.class);
    }

    @Override
    public OutputAnalysis analyzeOutput(SourceRetrievalResult sourceRetrievalResult, LineHandler stdoutHandler, LineHandler stderrLineHandler) throws IOException {
        Map<File, List<CheckstyleError>> checkstyleErrors = new HashMap<>();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        stdoutHandler.handle("Starting to parse checkstyle xmls");
        final List<File> xmls = new ArrayList<>();
        Files.walkFileTree(Paths.get(sourceRetrievalResult.getHeadDir().toURI()), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                File file = path.toFile();
                if ("checkstyle-result.xml".equals(file.getName())) {
                    xmls.add(file);
                }
                return super.visitFile(path, attrs);
            }
        });
        for (File file : xmls) {
            Document document;
            try {
                document = documentBuilderFactory.newDocumentBuilder().parse(file);
            } catch (Exception e) {
                throw new IOException(e);
            }
            NodeList childNodes = document.getFirstChild().getChildNodes();
            Set<Node> files = new HashSet<>();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if ("file".equals(node.getNodeName())) {
                    files.add(node);
                }
            }
            for (Node fileNode : files) {
                NodeList fileNodeChildNodes = fileNode.getChildNodes();
                List<CheckstyleError> fileErrors = new ArrayList<>();
                for (int i = 0; i < fileNodeChildNodes.getLength(); i++) {
                    Node fileNodeChildNode = fileNodeChildNodes.item(i);
                    if ("error".equals(fileNodeChildNode.getNodeName())) {
                        NamedNodeMap fileNodeAttributes = fileNodeChildNode.getAttributes();
                        Integer line = null;
                        try {
                            line = Integer.parseInt(fileNodeAttributes.getNamedItem("line").getNodeValue());
                        } catch (Exception e) {
                            // ignore
                        }
                        Integer column = null;
                        try {
                            column = Integer.parseInt(fileNodeAttributes.getNamedItem("column").getNodeValue());
                        } catch (Exception e) {
                            // ignore
                        }
                        String message = fileNodeAttributes.getNamedItem("message").getNodeValue();
                        fileErrors.add(new CheckstyleError(line, column, message));
                    }
                }
                if (fileErrors.size() > 0) {
                    checkstyleErrors.put(new File(fileNode.getAttributes().getNamedItem("name").getNodeValue()), fileErrors);
                }
            }
        }
        stdoutHandler.handle("Done parsing checkstyle xmls");
        Set<File> changedFiles = new HashSet<>();
        for (OutputChangeDetector outputChangeDetector : outputChangeDetectors) {
            changedFiles.addAll(outputChangeDetector.hasChanged(sourceRetrievalResult.getChangedFiles(), new HashSet<>(checkstyleErrors.keySet())));
        }
        List<File> changedFileList = new ArrayList<>(changedFiles);
        Collections.sort(changedFileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
            }
        });
        final List<CheckstyleFileErrors> checkstyleFileErrors = new ArrayList<>();
        for (File file : changedFileList) {
            checkstyleFileErrors.add(new CheckstyleFileErrors(file.getAbsolutePath(), checkstyleErrors.get(file)));
        }
        final String string = ftlUtil.render("checkstyle.ftl", "violations", checkstyleFileErrors);

        return new OutputAnalysis.Builder()
            .severity(checkstyleFileErrors.size() > 0 ? OutputSeverity.ERROR : OutputSeverity.INFO)
            .report(string)
            .url("http://wiki.pentaho.com/display/PEOpen/Code+Formatting+and+CheckStyle")
            .build();
    }

    @Override
    public String getDescription() {
        return "The code must adhere to the style guidelines.";
    }

    @Override
    public String getId() {
        return "Checkstyle";
    }
}
