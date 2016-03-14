package org.pentaho.build.buddy.bundles.analyzer.tests;

import java.io.IOException;
import java.util.*;

/**
 * Created by bryan on 3/8/16.
 */
public class TestSuite {
    private final List<TestError> suiteErrors = new ArrayList<>();
    private final Map<String, Map<String, TestError>> caseErrors = new HashMap<>();

    public boolean addSuiteError(TestError suiteError) {
        return suiteErrors.add(suiteError);
    }

    public boolean addSuiteErrors(Collection<TestError> suiteErrors) {
        return this.suiteErrors.addAll(suiteErrors);
    }

    public void addCaseError(String className, String name, TestError caseError) throws IOException {
        Map<String, TestError> classErrors = caseErrors.get(className);
        if (classErrors == null) {
            classErrors = new HashMap<>();
            caseErrors.put(className, classErrors);
        }
        TestError put = classErrors.put(name, caseError);
        if (put != null) {
            throw new IOException("Already had an error for class " + className + "." + name);
        }
    }

    boolean hasErrors() {
        return suiteErrors.size() > 0 || caseErrors.size() > 0;
    }

    public List<TestError> getSuiteErrors() {
        return Collections.unmodifiableList(suiteErrors);
    }

    public Map<String, Map<String, TestError>> getCaseErrors() {
        Map<String, Map<String, TestError>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, TestError>> stringMapEntry : caseErrors.entrySet()) {
            result.put(stringMapEntry.getKey(), Collections.unmodifiableMap(stringMapEntry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }
}
