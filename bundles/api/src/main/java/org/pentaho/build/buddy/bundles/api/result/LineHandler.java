package org.pentaho.build.buddy.bundles.api.result;

/**
 * Created by bryan on 2/17/16.
 */
public interface LineHandler {
    void handle(String line);

    void handle(Exception e);
}
