package org.pentaho.build.buddy.bundles.orchestrator;

import org.pentaho.build.buddy.bundles.api.output.OutputAnalyzer;
import org.junit.Test;
import org.pentaho.build.buddy.bundles.orchestrator.OutputAnalyzerWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by bryan on 3/10/16.
 */
public class OutputAnalyzerWrapperTest {
    @Test
    public void testSort() {
        OutputAnalyzer firstRank = mock(OutputAnalyzer.class);
        OutputAnalyzer tieBreakerFirst = mock(OutputAnalyzer.class);
        OutputAnalyzer tieBreakerSecond = mock(OutputAnalyzer.class);
        OutputAnalyzer lastRank = mock(OutputAnalyzer.class);

        when(firstRank.toString()).thenReturn("zed");
        when(tieBreakerFirst.toString()).thenReturn("a");
        when(tieBreakerSecond.toString()).thenReturn("b");
        when(lastRank.toString()).thenReturn("aaa");

        List<OutputAnalyzerWrapper> outputAnalyzerWrappers = new ArrayList<>();
        outputAnalyzerWrappers.add(new OutputAnalyzerWrapper(lastRank, 20));
        outputAnalyzerWrappers.add(new OutputAnalyzerWrapper(firstRank, 40));
        outputAnalyzerWrappers.add(new OutputAnalyzerWrapper(tieBreakerSecond, 30));
        outputAnalyzerWrappers.add(new OutputAnalyzerWrapper(tieBreakerFirst, 30));

        Collections.sort(outputAnalyzerWrappers);

        assertEquals(4, outputAnalyzerWrappers.size());
        assertEquals(firstRank, outputAnalyzerWrappers.get(0).getOutputAnalyzer());
        assertEquals(tieBreakerFirst, outputAnalyzerWrappers.get(1).getOutputAnalyzer());
        assertEquals(tieBreakerSecond, outputAnalyzerWrappers.get(2).getOutputAnalyzer());
        assertEquals(lastRank, outputAnalyzerWrappers.get(3).getOutputAnalyzer());
    }
}
