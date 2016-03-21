package org.pentaho.build.buddy.bundles.analyzer.tests;

import java.io.IOException;
import java.util.*;

/**
 * Created by bryan on 3/8/16.
 */
public class TestSuite {
    private final List<TestError> suiteErrors = new ArrayList<>();
    private final Map<String, Map<String, List<TestError>>> caseErrors = new HashMap<>();

    public boolean addSuiteError(TestError suiteError) {
        return suiteErrors.add(suiteError);
    }

    public boolean addSuiteErrors(Collection<TestError> suiteErrors) {
        return this.suiteErrors.addAll(suiteErrors);
    }

    public void addCaseError(String className, String name, TestError caseError) throws IOException {
        Map<String, List<TestError>> classErrors = caseErrors.get(className);
        if (classErrors == null) {
            classErrors = new HashMap<>();
            caseErrors.put(className, classErrors);
        }
        List<TestError> caseErrors = classErrors.get(name);
        if (caseErrors == null) {
            caseErrors = new ArrayList<>();
            classErrors.put(name, caseErrors);
        }
        caseErrors.add(caseError);
    }

    public void addCaseErrors(String className, String name, List<TestError> caseErrors) throws IOException {
        for (TestError caseError : caseErrors) {
            addCaseError(className, name, caseError);
        }
    }

    boolean hasErrors() {
        return suiteErrors.size() > 0 || caseErrors.size() > 0;
    }

    public List<TestError> getSuiteErrors() {
        return Collections.unmodifiableList(suiteErrors);
    }

    public Map<String, Map<String, List<TestError>>> getCaseErrors() {
        Map<String, Map<String, List<TestError>>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, List<TestError>>> stringMapEntry : caseErrors.entrySet()) {
            Map<String, List<TestError>> classResult = new HashMap<>();
            for (Map.Entry<String, List<TestError>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                classResult.put(stringListEntry.getKey(), Collections.unmodifiableList(new ArrayList<>(stringListEntry.getValue())));
            }
            result.put(stringMapEntry.getKey(), Collections.unmodifiableMap(classResult));
        }
        return Collections.unmodifiableMap(result);
    }
}
