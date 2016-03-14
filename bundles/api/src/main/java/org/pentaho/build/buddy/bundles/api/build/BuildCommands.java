package org.pentaho.build.buddy.bundles.api.build;

import java.util.List;

/**
 * Created by bryan on 2/26/16.
 */
public interface BuildCommands {
    List<String> getCommands();
    String getCleanupCommand();
}
