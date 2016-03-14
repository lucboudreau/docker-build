package org.pentaho.build.buddy.bundles.api.build;

import java.io.File;

/**
 * Created by bryan on 3/3/16.
 */
public interface BuildMetadata {
    File getWorkingDirectory();

    BuildCommands getBuildCommands();
}
