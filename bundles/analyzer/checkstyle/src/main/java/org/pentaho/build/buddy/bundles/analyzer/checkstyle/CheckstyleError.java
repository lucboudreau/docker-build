package org.pentaho.build.buddy.bundles.analyzer.checkstyle;

/**
 * Created by bryan on 3/7/16.
 */
public class CheckstyleError {
    private final Integer line;
    private final Integer column;
    private final String message;

    public CheckstyleError(Integer line, Integer column, String message) {
        this.line = line;
        this.column = column;
        this.message = message;
    }

    public Integer getLine() {
        return line;
    }

    public Integer getColumn() {
        return column;
    }

    public String getMessage() {
        return message;
    }
}
