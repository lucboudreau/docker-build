package org.pentaho.build.buddy.bundles.analyzer.tests;

import java.io.IOException;
import java.util.*;

/**
 * Created by bryan on 3/8/16.
 */
public class TestSuiteDiff {
    private final TestSuite newlyBroken;
    private final TestSuite newlyFixed;
    private final TestSuite stillBroken;

    public TestSuiteDiff(TestSuite baseTestSuite, TestSuite headTestSuite) throws IOException {
        stillBroken = new TestSuite();
        if (baseTestSuite != null && headTestSuite != null) {
            newlyBroken = new TestSuite();
            newlyFixed = new TestSuite();
            List<TestError> baseTestSuiteSuiteErrors = baseTestSuite.getSuiteErrors();
            List<TestError> headTestSuiteSuiteErrors = headTestSuite.getSuiteErrors();
            Set<TestError> headErrorSet = new HashSet<>(headTestSuiteSuiteErrors);

            for (TestError baseTestSuiteSuiteError : baseTestSuiteSuiteErrors) {
                if (headErrorSet.remove(baseTestSuiteSuiteError)) {
                    stillBroken.addSuiteError(baseTestSuiteSuiteError);
                } else {
                    newlyFixed.addSuiteError(baseTestSuiteSuiteError);
                }
            }
            for (TestError headTestSuiteSuiteError : headTestSuiteSuiteErrors) {
                if (headErrorSet.contains(headTestSuiteSuiteError)) {
                    newlyBroken.addSuiteError(headTestSuiteSuiteError);
                }
            }

            Map<String, Map<String, List<TestError>>> baseTestSuiteCaseErrors = baseTestSuite.getCaseErrors();
            Map<String, Map<String, List<TestError>>> headTestSuiteCaseErrors = new HashMap<>(headTestSuite.getCaseErrors());

            for (Map.Entry<String, Map<String, List<TestError>>> stringMapEntry : baseTestSuiteCaseErrors.entrySet()) {
                Map<String, List<TestError>> headTestCaseMap = headTestSuiteCaseErrors.remove(stringMapEntry.getKey());
                if (headTestCaseMap != null) {
                    headTestCaseMap = new HashMap<>(headTestCaseMap);
                    for (Map.Entry<String, List<TestError>> stringTestErrorEntry : stringMapEntry.getValue().entrySet()) {
                        List<TestError> testError = headTestCaseMap.remove(stringTestErrorEntry.getKey());
                        if (testError != null) {
                            stillBroken.addCaseErrors(stringMapEntry.getKey(), stringTestErrorEntry.getKey(), testError);
                        } else {
                            newlyFixed.addCaseErrors(stringMapEntry.getKey(), stringTestErrorEntry.getKey(), stringTestErrorEntry.getValue());
                        }
                    }
                    for (Map.Entry<String, List<TestError>> stringTestErrorEntry : headTestCaseMap.entrySet()) {
                        newlyBroken.addCaseErrors(stringMapEntry.getKey(), stringTestErrorEntry.getKey(), stringTestErrorEntry.getValue());
                    }
                } else {
                    for (Map.Entry<String, List<TestError>> testErrorEntry : stringMapEntry.getValue().entrySet()) {
                        newlyFixed.addCaseErrors(stringMapEntry.getKey(), testErrorEntry.getKey(), testErrorEntry.getValue());
                    }
                }
            }
            for (Map.Entry<String, Map<String, List<TestError>>> stringMapEntry : headTestSuiteCaseErrors.entrySet()) {
                for (Map.Entry<String, List<TestError>> stringTestErrorEntry : stringMapEntry.getValue().entrySet()) {
                    newlyBroken.addCaseErrors(stringMapEntry.getKey(), stringTestErrorEntry.getKey(), stringTestErrorEntry.getValue());
                }
            }

        } else if (baseTestSuite == null) {
            newlyBroken = headTestSuite;
            newlyFixed = new TestSuite();
        } else if (headTestSuite == null) {
            newlyBroken = new TestSuite();
            newlyFixed = baseTestSuite;
        } else {
            throw new IllegalArgumentException("Expected non-null base or head test suite");
        }
    }

    public TestSuite getNewlyBroken() {
        return newlyBroken;
    }

    public TestSuite getNewlyFixed() {
        return newlyFixed;
    }

    public TestSuite getStillBroken() {
        return stillBroken;
    }
}
