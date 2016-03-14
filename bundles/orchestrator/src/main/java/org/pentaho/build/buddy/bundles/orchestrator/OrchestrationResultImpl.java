package org.pentaho.build.buddy.bundles.orchestrator;

import org.pentaho.build.buddy.bundles.api.orchestrator.OrchestrationResult;

/**
 * Created by bryan on 3/10/16.
 */
public class OrchestrationResultImpl implements OrchestrationResult {
    private final String report;
    private final Status status;

    public OrchestrationResultImpl(Status status, String report) {
        this.report = report;
        this.status = status;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public String getReport() {
        return report;
    }
}
