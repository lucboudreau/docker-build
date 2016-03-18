package org.pentaho.build.buddy.bundles.command.mvn;

import org.pentaho.build.buddy.bundles.api.build.BuildCommands;
import org.pentaho.build.buddy.bundles.api.build.CommandBuilder;
import org.pentaho.build.buddy.bundles.api.build.impl.BuildCommandsImpl;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.bundles.util.config.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by bryan on 2/26/16.
 */
public class MvnCommandBuilder implements CommandBuilder {
    public static final String MVN = "mvn";
    private static final Logger logger = LoggerFactory.getLogger(MvnCommandBuilder.class);

    @Override
    public BuildCommands buildCommands(SourceRetrievalResult sourceRetrievalResult, final Map config) throws IOException {
        final List<String> result = new ArrayList<>();
        String beforeAll = MapUtil.getStringOrNull(config, BEFORE_ALL);
        if (beforeAll != null) {
            result.add(beforeAll);
        }
        String command = MapUtil.getStringOrNull(config, COMMAND);
        if (command == null) {
            command = "mvn -B -f BUILD_FILE clean install site";
        }

        final String cleanupCommand = MapUtil.getStringOrNull(config, CLEANUP_COMMAND);

        if (!MapUtil.getValue(config, EXPAND, false)) {
            return new BuildCommandsImpl(beforeAll, command, cleanupCommand);
        }

        List<String> changedFiles = new ArrayList<>(sourceRetrievalResult.getChangedFiles());
        Collections.sort(changedFiles);

        File baseDir = sourceRetrievalResult.getBaseDir();
        File headDir = sourceRetrievalResult.getHeadDir();

        MavenModule baseMavenModule = MavenModule.buildModule(new File(baseDir, "pom.xml"));
        MavenModule headMavenModule = MavenModule.buildModule(new File(headDir, "pom.xml"));

        if (logger.isDebugEnabled()) {
            logger.debug(baseMavenModule.toString());
            logger.debug(headMavenModule.toString());
        }

        Set<MavenModule> baseModulesToBuild = new HashSet<>();
        Set<MavenModule> headModulesToBuild = new HashSet<>();

        for (String changedFile : changedFiles) {
            String[] splitPath = changedFile.split("/");
            MavenModule baseModule = baseMavenModule.getMostSpecificModule(splitPath);
            MavenModule headModule = headMavenModule.getMostSpecificModule(splitPath);

            while (baseModule.getPath().length() > headModule.getPath().length()) {
                baseModule = baseModule.getParent();
            }
            while (headModule.getPath().length() > baseModule.getPath().length()) {
                headModule = headModule.getParent();
            }

            baseModulesToBuild.add(baseModule);
            headModulesToBuild.add(headModule);
        }

        Set<MavenModule> baseModulesToRemove = new HashSet<>();
        for (MavenModule mavenModule : baseModulesToBuild) {
            baseModulesToRemove.addAll(mavenModule.getAllDescendentModules());
        }
        baseModulesToBuild.removeAll(baseModulesToRemove);

        Set<MavenModule> headModulesToRemove = new HashSet<>();
        for (MavenModule mavenModule : headModulesToBuild) {
            headModulesToRemove.addAll(mavenModule.getAllDescendentModules());
        }
        headModulesToBuild.removeAll(headModulesToRemove);

        if (logger.isDebugEnabled()) {
            logger.debug("Base modules to build: " + baseModulesToBuild);
            logger.debug("Head modules to build: " + headModulesToBuild);
        }

        List<MavenModule> sortedHeadModulesToBuild = new ArrayList<>();
        if (headModulesToBuild.contains(headMavenModule)) {
            sortedHeadModulesToBuild.add(headMavenModule);
        } else {
            for (MavenModule mavenModule : headMavenModule.getAllDescendentModules()) {
                if (headModulesToBuild.contains(mavenModule)) {
                    sortedHeadModulesToBuild.add(mavenModule);
                }
            }
        }

        for (MavenModule mavenModule : sortedHeadModulesToBuild) {
            String relativePathToPom = Paths.get(headDir.toURI()).relativize(Paths.get(mavenModule.getPom().toURI())).toString();
            if (logger.isDebugEnabled()) {
                logger.debug("Adding pom to build: " + relativePathToPom);
            }
            result.add(command.replace("BUILD_FILE", relativePathToPom.toString()));
        }

        return new BuildCommandsImpl(result, cleanupCommand);
    }

    @Override
    public String getId() {
        return MVN;
    }
}
