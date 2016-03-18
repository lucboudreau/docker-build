package org.pentaho.build.buddy.bundles.analyzer.tests;

import org.w3c.dom.Node;

/**
 * Created by bryan on 3/8/16.
 */
public class TestError {
    private final String severity;
    private final String type;
    private final String message;

    public TestError(Node node, String severity) {
        this(severity, node.getAttributes().getNamedItem("type").getNodeValue(), node.getTextContent());
    }

    public TestError(String severity, String type, String message) {
        this.severity = severity;
        this.type = type;
        this.message = message.length() > 500 ? message.substring(0, 500): message;
    }

    public String getSeverity() {
        return severity;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestError testError = (TestError) o;

        if (severity != null ? !severity.equals(testError.severity) : testError.severity != null) return false;
        if (type != null ? !type.equals(testError.type) : testError.type != null) return false;
        return message != null ? message.equals(testError.message) : testError.message == null;

    }

    @Override
    public int hashCode() {
        int result = severity != null ? severity.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
