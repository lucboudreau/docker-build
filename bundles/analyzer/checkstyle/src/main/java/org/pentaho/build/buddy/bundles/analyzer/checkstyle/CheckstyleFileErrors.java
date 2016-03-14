package org.pentaho.build.buddy.bundles.analyzer.checkstyle;

import java.util.List;

/**
 * Created by bryan on 3/7/16.
 */
public class CheckstyleFileErrors {
    private final String fileName;
    private final List<CheckstyleError> checkstyleErrors;

    public CheckstyleFileErrors(String fileName, List<CheckstyleError> checkstyleErrors) {
        this.fileName = fileName;
        this.checkstyleErrors = checkstyleErrors;
    }

    public String getFileName() {
        return fileName;
    }

    public List<CheckstyleError> getCheckstyleErrors() {
        return checkstyleErrors;
    }
}
