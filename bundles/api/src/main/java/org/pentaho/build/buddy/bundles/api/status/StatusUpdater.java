package org.pentaho.build.buddy.bundles.api.status;

import org.pentaho.build.buddy.bundles.api.HasId;
import org.pentaho.build.buddy.bundles.api.orchestrator.OrchestrationResult;

import java.io.IOException;
import java.util.Map;

/**
 * Created by bryan on 3/11/16.
 */
public interface StatusUpdater extends HasId {
    String STATUS_UPDATER = "StatusUpdater";
    String STATUS_UPDATER_TYPE = "StatusUpdaterType";

    void onStart(Map config) throws IOException;

    void onStop(Map config, OrchestrationResult orchestrationResult) throws IOException;
}
