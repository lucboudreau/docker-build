package org.pentaho.build.buddy.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.pentaho.build.buddy.bundles.api.build.BuildCommands;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bryan on 3/3/16.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = BuildCommandsImpl.class)
public class BuildCommandsImpl implements BuildCommands {
    private final List<String> commands;
    private final String cleanupCommand;

    @JsonCreator
    public BuildCommandsImpl(@JsonProperty("commands") List<String> commands, @JsonProperty("cleanupCommand") String cleanupCommand) {
        this.commands = new ArrayList<>(commands);
        this.cleanupCommand = cleanupCommand;
    }

    @Override
    public List<String> getCommands() {
        return commands;
    }

    @Override
    public String getCleanupCommand() {
        return cleanupCommand;
    }

    @Override
    public String toString() {
        return "BuildCommandsImpl{" +
                "commands=" + commands +
                ", cleanupCommand='" + cleanupCommand + '\'' +
                '}';
    }
}
