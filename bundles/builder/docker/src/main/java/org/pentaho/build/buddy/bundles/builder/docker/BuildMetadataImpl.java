package org.pentaho.build.buddy.bundles.builder.docker;

import org.pentaho.build.buddy.bundles.api.build.BuildCommands;
import org.pentaho.build.buddy.bundles.api.build.BuildMetadata;

import java.io.File;

/**
 * Created by bryan on 3/7/16.
 */
public class BuildMetadataImpl implements BuildMetadata {
    private final File workingDirectory;
    private final BuildCommands buildCommands;

    public BuildMetadataImpl(File workingDirectory, BuildCommands buildCommands) {
        this.workingDirectory = workingDirectory;
        this.buildCommands = buildCommands;
    }

    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public BuildCommands getBuildCommands() {
        return buildCommands;
    }
}
