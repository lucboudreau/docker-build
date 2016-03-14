package org.pentaho.build.buddy.bundles.api.output;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Created by bryan on 3/7/16.
 */
public interface OutputChangeDetector {
    Set<File> hasChanged(List<String> changedFiles, Set<File> candidates);
}
