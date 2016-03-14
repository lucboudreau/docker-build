package org.pentaho.build.buddy.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.pentaho.build.buddy.bundles.api.build.BuildCommands;
import org.pentaho.build.buddy.bundles.api.build.BuildMetadata;

import java.io.File;

/**
 * Created by bryan on 3/3/16.
 */
public class BuildMetadataImpl implements BuildMetadata {
    private final File workingDirectory;
    private final BuildCommands buildCommands;

    public BuildMetadataImpl(@JsonProperty("workingDirectory") File workingDirectory, @JsonProperty("buildCommands") BuildCommands buildCommands) {
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

    @Override
    public String toString() {
        return "BuildMetadataImpl{" +
                "workingDirectory=" + workingDirectory +
                ", buildCommands=" + buildCommands +
                '}';
    }
}
