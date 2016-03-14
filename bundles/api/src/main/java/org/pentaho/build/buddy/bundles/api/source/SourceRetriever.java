package org.pentaho.build.buddy.bundles.api.source;

import org.pentaho.build.buddy.bundles.api.HasId;

import java.io.IOException;
import java.util.Map;

/**
 * Created by bryan on 2/16/16.
 */
public interface SourceRetriever extends HasId {
    String SOURCE_RETRIEVER = "SourceRetriever";

    String SOURCE_CONTROL_TYPE = "SourceControlType";

    SourceRetrievalResult retrieveSource(Map config) throws IOException;
}
