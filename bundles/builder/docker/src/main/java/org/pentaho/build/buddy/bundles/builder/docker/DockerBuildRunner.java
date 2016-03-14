package org.pentaho.build.buddy.bundles.builder.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pentaho.build.buddy.bundles.api.build.BuildCommands;
import org.pentaho.build.buddy.bundles.api.build.BuildRunner;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.util.config.MapUtil;
import org.pentaho.build.buddy.util.shell.ShellException;
import org.pentaho.build.buddy.util.shell.ShellUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bryan on 3/2/16.
 */
public class DockerBuildRunner implements BuildRunner {
    public static final String CONTAINER = "Container";
    public static final String VOLUMES = "Volumes";
    public static final String PROJECT_DIR = "ProjectDir";
    public static final String DOCKER = "Docker";
    private static final Logger logger = LoggerFactory.getLogger(DockerBuildRunner.class);
    public static final String THREAD_NAME = "THREAD_NAME";
    private final ShellUtil shellUtil;

    public DockerBuildRunner() {
        this(new ShellUtil());
    }

    public DockerBuildRunner(ShellUtil shellUtil) {
        this.shellUtil = shellUtil;
    }

    @Override
    public boolean canHandle(Map config) {
        return DOCKER.equalsIgnoreCase(String.valueOf(config.get(RUNNER_TYPE)));
    }

    @Override
    public int runBuild(File directory, BuildCommands buildCommands, Map config, LineHandler stdoutHandler, LineHandler stderrHandler) throws IOException {
        String dockerFile = MapUtil.getStringOrThrow(config, CONTAINER);
        Object volumeObj = config.get(VOLUMES);
        Map<String, String> volumes;
        if (volumeObj == null) {
            volumes = new HashMap<>();
        } else if (volumeObj instanceof Map) {
            volumes = new HashMap<>((Map) volumeObj);
        } else {
            throw new IOException("Expecting " + VOLUMES + " to be a map");
        }
        String projectDir = MapUtil.getStringOrNull(config, PROJECT_DIR);
        if (projectDir == null) {
            projectDir = "/home/buildguy/project";
        }
        volumes.put(directory.getAbsolutePath(), projectDir);
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("-i");
        command.add("--rm");
        for (Map.Entry<String, String> stringStringEntry : volumes.entrySet()) {
            command.add("-v");
            String key = stringStringEntry.getKey().replace(THREAD_NAME, Thread.currentThread().getName());
            new File(key).mkdirs();
            command.add(key + ":" + stringStringEntry.getValue());
        }
        command.add(dockerFile);
        command.add(new ObjectMapper().writeValueAsString(new BuildMetadataImpl(new File(projectDir), buildCommands)));
        logger.info(command.toString());

        try {
            return shellUtil.execute(null, stderrHandler, stdoutHandler, null, command.toArray(new String[command.size()]));
        } catch (InterruptedException e) {
            throw new IOException(e);
        } catch (ShellException e) {
            throw new IOException(e);
        }
    }
}
