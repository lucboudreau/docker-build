package org.pentaho.build.buddy.agent;

/**
 * Created by bryan on 3/7/16.
 */
public class BuildException extends Exception {
    private final int errorCode;

    public BuildException(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
