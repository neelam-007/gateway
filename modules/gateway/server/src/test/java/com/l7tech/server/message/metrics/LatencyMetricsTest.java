package com.l7tech.server.message.metrics;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit Tests for {@link LatencyMetrics}
 */
public class LatencyMetricsTest {
    LatencyMetrics latencyMetrics = new LatencyMetrics(100, 400);

    @Test
    public void testGetStartTimeMs() throws Exception {
        assertEquals("Start time should be 100ms", 100, latencyMetrics.getStartTimeMs());
    }

    @Test
    public void testGetEndTimeMs() throws Exception {
        assertEquals("End time should be 400ms", 400, latencyMetrics.getEndTimeMs());
    }

    @Test
    public void testGetLatencyMs() throws Exception {
        assertEquals("Latency should be 400 - 100 = 300ms", 300, latencyMetrics.getLatencyMs());
    }

}