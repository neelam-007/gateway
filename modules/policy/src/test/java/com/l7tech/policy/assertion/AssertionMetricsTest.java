package com.l7tech.policy.assertion;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit Tests for {@link AssertionMetrics}
 */
public class AssertionMetricsTest {
    AssertionMetrics assertionMetrics = new AssertionMetrics(100, 400);

    @Test
    public void testGetStartTimeMs() throws Exception {
        assertEquals("Start time should be 100ms", 100, assertionMetrics.getStartTimeMs());
    }

    @Test
    public void testGetEndTimeMs() throws Exception {
        assertEquals("End time should be 400ms", 400, assertionMetrics.getEndTimeMs());
    }

    @Test
    public void testgetLatencyMs() throws Exception {
        assertEquals("Latency should be 400 - 100 = 300ms", 300, assertionMetrics.getLatencyMs());
    }

}