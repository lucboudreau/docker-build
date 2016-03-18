package org.pentaho.build.buddy.bundles.orchestrator;

import org.pentaho.build.buddy.bundles.api.build.BuildCommands;
import org.pentaho.build.buddy.bundles.api.build.CommandBuilder;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bryan on 3/18/16.
 */
public class BuildToolDetector {
    public void tryToDetermineBuildTool(Map config, SourceRetrievalResult sourceRetrievalResult, LineHandler stdoutLineHandler) {
        Map commandBuilderConfig = (Map) config.get(CommandBuilder.COMMAND_BUILDER);
        if (commandBuilderConfig == null) {
            commandBuilderConfig = new HashMap();
        }
        String buildTool = (String) commandBuilderConfig.get(CommandBuilder.BUILD_TOOL);
        if (buildTool == null) {
            if (new File(sourceRetrievalResult.getHeadDir(), "pom.xml").exists()) {
                commandBuilderConfig.put(CommandBuilder.BUILD_TOOL, "mvn");
                config.put(CommandBuilder.COMMAND_BUILDER, commandBuilderConfig);
                stdoutLineHandler.handle(BuildToolDetector.class.getSimpleName() + " guessing mvn project because pom exists in root dir");
            } else if (new File(sourceRetrievalResult.getHeadDir(), "build.xml").exists()) {
                commandBuilderConfig.put(CommandBuilder.BUILD_TOOL, "ant");
                config.put(CommandBuilder.COMMAND_BUILDER, commandBuilderConfig);
                stdoutLineHandler.handle(BuildToolDetector.class.getSimpleName() + " guessing ant project because build.xml exists in root dir");
            } else {
                stdoutLineHandler.handle(BuildToolDetector.class.getSimpleName() + " unable to determine build type, falling back to config defaults");
            }
        }
    }
}
