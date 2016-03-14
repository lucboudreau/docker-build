package org.pentaho.build.buddy.bundles.api.orchestrator;

import org.pentaho.build.buddy.bundles.api.result.LineHandler;

import java.util.Map;

/**
 * Created by bryan on 2/17/16.
 */
public interface Orchestrator {
    OrchestrationResult orchestrate(Map config, LineHandler stdoutLineHandler, LineHandler stderrLineHandler);
}
