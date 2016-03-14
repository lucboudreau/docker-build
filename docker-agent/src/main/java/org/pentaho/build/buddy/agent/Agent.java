package org.pentaho.build.buddy.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pentaho.build.buddy.bundles.api.build.BuildCommands;
import org.pentaho.build.buddy.bundles.api.build.BuildMetadata;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.util.shell.ShellException;
import org.pentaho.build.buddy.util.shell.ShellUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by bryan on 3/3/16.
 */
public class Agent {
    public void runMain(BuildMetadata buildMetadata) throws InterruptedException, ShellException, IOException, BuildException {
        ShellUtil shellUtil = new ShellUtil();
        LineHandler lineHandler = new LineHandler() {

            @Override
            public void handle(String line) {
                System.out.println(line);
            }

            @Override
            public void handle(Exception e) {
                e.printStackTrace();
            }
        };

        File workingDirectory = buildMetadata.getWorkingDirectory();
        BuildCommands buildCommands = buildMetadata.getBuildCommands();
        try {
            for (String command : buildCommands.getCommands()) {
                int returnCode = shellUtil.executeCommandLineAndCheck(workingDirectory, lineHandler, lineHandler, null, command);
                if (returnCode != 0) {
                    throw new BuildException(returnCode);
                }
            }
        } finally {
            String cleanupCommand = buildCommands.getCleanupCommand();
            if (cleanupCommand != null) {
                shellUtil.executeCommandLineAndCheck(workingDirectory, lineHandler, lineHandler, null, cleanupCommand);
            }
        }

    }
    public static void main(String[] args) throws IOException, ShellException, InterruptedException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expecting BuildCommands payload as only argument, got " + Arrays.toString(args));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixIn(BuildCommands.class, BuildCommandsImpl.class);
        BuildMetadata buildMetadata = objectMapper.readerFor(BuildMetadataImpl.class).readValue(args[0]);

        try {
            new Agent().runMain(buildMetadata);
        } catch (BuildException e) {
            System.exit(e.getErrorCode());
        }
    }
}
