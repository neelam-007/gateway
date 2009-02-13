package com.l7tech.server.processcontroller.monitoring.sampling;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 */
public class CpuIdleSamplerTest {
    @Test
    public void testParseVmstat() throws Exception {
        int got = CpuUsageSampler.parseVmstatOutput(
                "procs -----------memory---------- ---swap-- -----io---- --system-- -----cpu------\n" +
                " r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st\n" +
                " 1  0   6592  18536  36088 839708    0    0     8    80    1    1  1  1 97  1  0\n" +
                " 1  0   6592  18536  36088 839708    0    0     8    80    1    1 15  8 76  1  0\n");

        assertEquals(76, got);
    }
}
