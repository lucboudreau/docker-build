package org.pentaho.build.buddy.bundles.api.source;

import java.io.File;
import java.util.List;

/**
 * Created by bryan on 2/16/16.
 */
public interface SourceRetrievalResult {
    File getBaseDir();

    File getHeadDir();

    List<String> getChangedFiles();
}
