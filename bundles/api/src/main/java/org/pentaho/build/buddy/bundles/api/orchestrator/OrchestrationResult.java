package org.pentaho.build.buddy.bundles.api.orchestrator;

/**
 * Created by bryan on 3/10/16.
 */
public interface OrchestrationResult {
    enum Status {
        GOOD,
        WARN,
        ERROR
    }

    Status getStatus();

    String getReport();
}
