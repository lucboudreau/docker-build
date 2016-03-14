package org.pentaho.build.buddy.bundles.command.ant;

import org.pentaho.build.buddy.bundles.api.build.BuildCommands;
import org.pentaho.build.buddy.bundles.api.build.CommandBuilder;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.bundles.util.config.MapUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by bryan on 3/11/16.
 */
public class AntCommandBuilder implements CommandBuilder {
    public static final String ANT = "ant";
    public static final String MULTIMODULE = "multimodule";

    @Override
    public BuildCommands buildCommands(SourceRetrievalResult sourceRetrievalResult, Map config) throws IOException {
        final List<String> result = new ArrayList<>();
        String beforeAll = MapUtil.getStringOrNull(config, BEFORE_ALL);
        if (beforeAll != null) {
            result.add(beforeAll);
        }
        String command = MapUtil.getStringOrNull(config, COMMAND);
        if (command == null) {
            command = "ant -Dpentaho.coding.standards.version=1.0.4 -Djunit.jvmargs=-XX:PermSize=256m -f BUILD_FILE clean-all resolve jacoco jacoco-integration checkstyle publish-local";
        }

        final List<String> baseBuildXmlDirs = new ArrayList<>();
        final Set<String> headBuildXmlDirs = new HashSet<>();

        final URI baseDirUri = sourceRetrievalResult.getBaseDir().toURI();
        Files.walkFileTree(Paths.get(baseDirUri), new BuildXmlVisitor(baseBuildXmlDirs, baseDirUri));

        final URI headDirUri = sourceRetrievalResult.getBaseDir().toURI();
        Files.walkFileTree(Paths.get(headDirUri), new BuildXmlVisitor(headBuildXmlDirs, headDirUri));

        Collections.sort(baseBuildXmlDirs, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int lengthDiff = o2.length() - o1.length();
                if (lengthDiff != 0) {
                    return lengthDiff;
                }
                return o1.compareTo(o2);
            }
        });

        Set<String> buildDirs = new HashSet<>();
        for (String changedFile : sourceRetrievalResult.getChangedFiles()) {
            boolean found = false;
            for (String baseBuildXmlDir : baseBuildXmlDirs) {
                if (changedFile.startsWith(baseBuildXmlDir) && headBuildXmlDirs.contains(baseBuildXmlDir)) {
                    buildDirs.add(baseBuildXmlDir);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IOException("Unable to determine common build directory for " + changedFile);
            }
        }
        List<String> buildDirsList = new ArrayList<>(buildDirs);
        List<String> multimoduleOrder = (List<String>) config.get(MULTIMODULE);
        if (multimoduleOrder == null) {
            multimoduleOrder = new ArrayList<>();
        }
        final List<String> finalMultimoduleOrder = multimoduleOrder;
        Collections.sort(buildDirsList, new Comparator<String>() {
            private int index(String str) {
                for (int i = 0; i < finalMultimoduleOrder.size(); i++) {
                    if (str.startsWith(finalMultimoduleOrder.get(i))) {
                        return i;
                    }
                }
                return finalMultimoduleOrder.size();
            }
            @Override
            public int compare(String o1, String o2) {
                int o1Index = index(o1);
                int o2Index = index(o2);
                if (o1Index != o2Index) {
                    return o1Index - o2Index;
                }
                return o1.compareTo(o2);
            }
        });

        for (String s : buildDirsList) {
            result.add(command.replace("BUILD_FILE", s + "/build.xml"));
        }

        final String cleanupCommand = MapUtil.getStringOrNull(config, CLEANUP_COMMAND);

        return new BuildCommands() {
            @Override
            public List<String> getCommands() {
                return result;
            }

            @Override
            public String getCleanupCommand() {
                return cleanupCommand;
            }
        };
    }

    @Override
    public String getId() {
        return ANT;
    }

    private static class BuildXmlVisitor extends SimpleFileVisitor<Path> {
        private final Collection<String> buildXmls;
        private final URI baseUri;

        private BuildXmlVisitor(Collection<String> buildXmls, URI baseUri) {
            this.buildXmls = buildXmls;
            this.baseUri = baseUri;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            File toFile = file.toFile();
            if ("build.xml".equals(toFile.getName())) {
                String path = baseUri.relativize(toFile.getParentFile().toURI()).getPath();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                buildXmls.add(path);
            }
            return super.visitFile(file, attrs);
        }
    }
}
