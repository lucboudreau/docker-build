package org.pentaho.build.buddy.bundles.change.standard;

import org.pentaho.build.buddy.bundles.api.output.OutputChangeDetector;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by bryan on 3/7/16.
 */
public class StandardChangeDetector implements OutputChangeDetector {
    @Override
    public Set<File> hasChanged(List<String> changedFiles, Set<File> candidates) {
        Set<File> result = new HashSet<>();
        for (File candidate : candidates) {
            for (String changedFile : changedFiles) {
                if (candidate.getAbsolutePath().endsWith(changedFile)) {
                    result.add(candidate);
                    break;
                }
            }
        }
        return result;
    }
}
