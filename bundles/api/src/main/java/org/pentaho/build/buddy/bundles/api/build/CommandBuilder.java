package org.pentaho.build.buddy.bundles.api.build;

import org.pentaho.build.buddy.bundles.api.HasId;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;

import java.io.IOException;
import java.util.Map;

/**
 * Created by bryan on 2/26/16.
 */
public interface CommandBuilder extends HasId {
    String COMMAND_BUILDER = "CommandBuilder";
    String BUILD_TOOL = "BuildTool";
    String BEFORE_ALL = "BeforeAll";
    String COMMAND = "BuildCommand";
    String CLEANUP_COMMAND = "CleanupCommand";
    String EXPAND = "Expand";

    BuildCommands buildCommands(SourceRetrievalResult sourceRetrievalResult, Map config) throws IOException;
}
