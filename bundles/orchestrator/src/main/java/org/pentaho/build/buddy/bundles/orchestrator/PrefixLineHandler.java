package org.pentaho.build.buddy.bundles.orchestrator;

import org.pentaho.build.buddy.bundles.api.result.LineHandler;

/**
 * Created by bryan on 3/7/16.
 */
public class PrefixLineHandler implements LineHandler {
    private final LineHandler delegate;
    private final String prefix;

    public PrefixLineHandler(LineHandler delegate, String prefix) {
        this.delegate = delegate;
        this.prefix = prefix;
    }

    @Override
    public void handle(String line) {
        delegate.handle(prefix + line);
    }

    @Override
    public void handle(Exception e) {
        delegate.handle(new Exception(prefix + e.getMessage(), e));
    }
}
