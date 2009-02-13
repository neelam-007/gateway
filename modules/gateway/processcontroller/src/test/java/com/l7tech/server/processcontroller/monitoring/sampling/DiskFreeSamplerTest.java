/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 */
public class DiskFreeSamplerTest {
    @Test
    public void testMatch() throws PropertySamplingException {
        String input = "Filesystem           1K-blocks      Used Available Use% Mounted on\n" +
                       "/dev/sda2              8064304   5224840   2429808  69% /";

        long matched = new DiskFreeSampler("blah").matchNumber(input, "df", DiskFreeSampler.DF_MATCHER);
        assertEquals(matched, 2429808);
    }
}
