/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.l7tech.util.TimeUnit;

public class TimeUnitTest extends TestCase {
    public void testParser() throws Exception {
        assertEquals("60s == one minute", TimeUnit.parse("60s"), 60 * 1000);
        assertEquals("1000(default ms) == one second", TimeUnit.parse("1000"), 1000);
        assertEquals("12m == 720000 ms", TimeUnit.parse("12m"), 720000);
        assertEquals("1d == 86400000ms", TimeUnit.parse("1d"), 86400000);
        assertEquals("1,000,000 == 1000s", TimeUnit.parse("1,000,000"), 1000000);
        assertEquals("1.5s == 1500ms", TimeUnit.parse("1.5s"), 1500);
        assertEquals(".5s == 500ms", TimeUnit.parse(".5s"), 500);
        assertEquals(".5d == 43200000", TimeUnit.parse(".5d"), 43200000);
        assertEquals("-1s == -1000s", TimeUnit.parse("-1s"), -1000);
        assertEquals("0 == 0", TimeUnit.parse("0"), 0);
        assertEquals("0.0 == 0", TimeUnit.parse("0.0"), 0);
        assertEquals(".000 == 0", TimeUnit.parse(".000"), 0);
        TimeUnit.parse("12345678901234567890"); // Ensure 20-digit input passes
        try { TimeUnit.parse("-12345678901234567890"); fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { TimeUnit.parse("0.0.0"); fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { TimeUnit.parse("-"); fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { TimeUnit.parse(""); fail("Expected exception was not thrown"); } catch (Exception e) {}
        try { TimeUnit.parse(null); fail("Expected exception was not thrown"); } catch (Exception e) {}
    }

    public TimeUnitTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TimeUnitTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}