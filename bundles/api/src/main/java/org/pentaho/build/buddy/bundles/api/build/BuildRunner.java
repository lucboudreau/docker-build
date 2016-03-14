package org.pentaho.build.buddy.bundles.api.build;

import org.pentaho.build.buddy.bundles.api.result.LineHandler;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by bryan on 2/16/16.
 */
public interface BuildRunner {
    String BUILD_RUNNER = "BuildRunner";
    String RUNNER_TYPE = "RunnerType";

    boolean canHandle(Map config);

    int runBuild(File directory, BuildCommands buildCommands, Map config, LineHandler stdoutHandler, LineHandler stderrHandler) throws IOException;
}
