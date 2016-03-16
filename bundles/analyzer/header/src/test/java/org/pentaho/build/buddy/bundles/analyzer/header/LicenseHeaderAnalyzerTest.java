package org.pentaho.build.buddy.bundles.analyzer.header;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by bryan on 3/16/16.
 */
public class LicenseHeaderAnalyzerTest {
    private LicenseHeaderAnalyzer licenseHeaderAnalyzer;
    private List<String> violations;
    private List<String> output;
    private LineHandler lineHandler;

    @Before
    public void setup() {
        licenseHeaderAnalyzer = new LicenseHeaderAnalyzer();
        violations = new ArrayList<>();
        output = new ArrayList<>();
        lineHandler = new LineHandler() {

            @Override
            public void handle(String line) {
                output.add(line);
            }

            @Override
            public void handle(Exception e) {
                output.add(e.getMessage());
            }
        };
    }

    private void analyzeFile(String name) throws URISyntaxException, IOException {
        File file = new File(LicenseHeaderAnalyzerTest.class.getClassLoader().getResource(name).toURI());
        licenseHeaderAnalyzer.analyzeFile(violations, name, file, lineHandler);
    }

    @Test
    public void testApacheBdp() throws URISyntaxException, IOException {
        analyzeFile("apache-bdp");
        assertEquals(0, violations.size());
        assertEquals(0, output.size());
    }

    @Test
    public void testApacheBdpBadLicense() throws URISyntaxException, IOException {
        analyzeFile("apache-bdp-bad-license");
        assertEquals(1, violations.size());
        assertEquals(1, output.size());
    }
}
